package models.checkout

import com.google.inject.Inject
import exception.InsufficientInventoryException
import java.sql.{Connection, Timestamp}
import models._
import models.enums._
import checkout.Conversions._
import services.AppConfig
import services.db._
import services.payment.Charge



//
// Services
//
case class CheckoutServices @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore,
  customerStore: CustomerStore,
  accountStore: AccountStore,
  addressStore: AddressStore,
  dbSession: DBSession,
  @CurrentTransaction currentTxnConnectionFactory: () => Connection
) extends InsertsAndUpdatesAsEntity[Checkout, CheckoutEntity] with SavesCreatedUpdated[CheckoutEntity] {
  import org.squeryl.PrimitiveTypeMode._

  override protected def table = schema.checkouts

  override def modelWithNewEntity(checkout: Checkout, entity: CheckoutEntity): PersistedCheckout = {
    checkout.withSavedEntity(entity)
  }

  override def withCreatedUpdated(toUpdate: CheckoutEntity, created: Timestamp, updated: Timestamp)
  : CheckoutEntity = { toUpdate.copy(created=created, updated=updated) }

  def findById(id: Long): Option[PersistedCheckout] = table.lookup(id).map( PersistedCheckout(_) )

  def getCheckoutsByCustomerId(id: Long): Seq[PersistedCheckout] = {
    table.where(entity => entity.customerId === id)
      .flatMap(entity => findById(entity.id)).toSeq
  }
}



//
// Base Model
//
abstract class Checkout extends CanInsertAndUpdateAsThroughServices[Checkout, CheckoutEntity]
  with HasEntity[CheckoutEntity, Long]
{
  import Checkout._

  //
  // Members
  //
  def _entity: CheckoutEntity
  def services: CheckoutServices
  def customer: Option[Customer]
  def zipcode: Option[String]
  def lineItems: LineItems
  def itemTypes: LineItemTypes
  def pendingItems: LineItems
  def pendingTypes: LineItemTypes
  protected def _persisted: Boolean
  protected def _dirty: Boolean     // simplifies transaction -- does nothing if clean

  /**
   * Types derived from zipcode and _addedTypes to be applied only to _addTypes (e.g. taxes, fees).
   * Needed to update things like taxes and fees, which are dependent on individual elements, when
   * adding new elements (ex: refunding taxes or including taxes on checkout edits).
   */
  lazy val _derivedTypes: LineItemTypes = if (!_dirty) { Nil } else {
    // TODO(refunds): will probably want to add the refund transaction here
    // TODO(fees): will want to add any fees we charge here
    TaxLineItemType.getTaxesByZip(zipcode.getOrElse(TaxLineItemType.noZipcode))
  }


  //
  // Methods
  //
  /** add additional types to this checkout -- for updating existing checkout */
  def withAdditionalTypes(newTypes: LineItemTypes): Checkout

  /** for services to return a persisted checkout as the correct type */
  def withSavedEntity(savedEntity: CheckoutEntity): PersistedCheckout

  def flattenLineItems: LineItems = { lineItems.flatMap(_.flatten) }

  /**
   * Charges or credits customer as needed through the information in the txnType, transacts all the
   * line items which have been added since the last transaction as well as the cash transaction
   * of the charge or credit to the customer, and returns the resulting PersistedCheckout.
   *
   * @param txnType used to charge or credit customer
   * @return failure, this if not _dirty, or the resulting PersistedCheckout
   */
  def transact(txnType: Option[CashTransactionLineItemType]): FailureOrCheckout = {
    if (pendingTypes.isEmpty){
      Right(this)

    } else if (!(balance.amount.isZero || txnType.isDefined)) {
      Left(CheckoutFailedCashTransactionMissing(this))

    } else {
      /**
       * Set savepoint to rollback to if transaction fails at some point after the checkout is
       * saved. For example, if a product is sold out, the whole checkout will be rolled back
       * because we don't want to charge the customer for products that didn't get stored or not
       * charge them for products that did.
       */
      val conn = services.currentTxnConnectionFactory()
      val savepoint = conn.setSavepoint()

      // save checkout and make charge
      val savedCheckout = if (_persisted) this.update() else this.insert()
      val txnItem = txnType.flatMap(_.lineItems( Seq(balance) )) match {
        // Type-erasure on Seq(item) not an issue unless more Payment-natured line items are added
        case Some(Seq(item: CashTransactionLineItem)) => Some( item.makeCharge(savedCheckout) )
        case _ => return Left{ CheckoutFailedCashTransactionResolutionError(this, txnType) }
      }

      try {
        // transact the new contents of the checkout, plus the cash transaction
        (txnItem ++ pendingItems).map(_.transact(savedCheckout))
      } catch {
        // refund charge, rollback any of the newly inserted items that didn't fail, and return failure details
        case e: InsufficientInventoryException => conn.rollback(savepoint)
          return Left{ CheckoutFailedInsufficientInventory(this, txnItem, txnItem.flatMap(_.abortTransaction())) }

        case e: Exception => conn.rollback(savepoint)
          return Left{ CheckoutFailedError(this, txnItem, txnItem.flatMap(_.abortTransaction())) }
      }

      Right(PersistedCheckout(savedCheckout._entity))
    }
  }

  /**
   * Resolves the given LineItemTypes into LineItems. Complexity is proportional to the sum of
   * unresolved types in each pass, which is O(n^2) in the worst case, but typically much better
   * than that, and very unlikely to affect application performance since Checkouts are unlikely
   * to become very large.
   *
   * @param types
   * @param preexistingItems
   * @return
   */
  protected def resolveTypes(types: LineItemTypes, preexistingItems: LineItems = Nil): LineItems = {

    case class ResolutionPass(resolved: LineItems = preexistingItems, unresolved: LineItemTypes) {

      /** execute a pass over the unresolved items, return resulting ResolutionPass */
      def execute: ResolutionPass = {
        /**
         * Iterates over unresolved types in O(n) time (compared to O(n^2) time of simply iterating
         * and filtering out current element to pass rest into its lineItems method).
         *
         * @param acc - accumulator of line items, should include previously resolved
         * @param before - item types preceding current type in iteration from list of all types
         * @param curr - current line item type being iterated over
         * @param after - remaining line item types to be iterated over
         * @return - ResolutionPass of resulting resolved items and unresolved types
         */
        def iterate(acc: LineItems = resolved)(     // accumulator is initially the already resolved items
          before: LineItemTypes = Nil,              // first iteration has nothing 'before' current type
          curr: LineItemType[_] = unresolved.head,  // current type in pass is initially the head
          after: LineItemTypes = unresolved.tail    // naturally, tail
        ): ResolutionPass = {

          /**
           * if current type resolves, add result to acc, otherwise add current type to before (save for next pass)
           * if more types exist to be iterated, recurse, otherwise return as resolution pass
           */
          val currResolved = curr.lineItems(acc, before ++ after)
          (after, currResolved) match {
            case (Nil, None) => ResolutionPass(acc, curr +: before)
            case (Nil, Some(res)) => ResolutionPass(res ++ acc,  before)
            case (next :: rest, None) => iterate(acc)(curr +: before, next, rest)
            case (next :: rest, Some(res)) => iterate(res ++ acc)( before, next, rest)
          }
        }

        if (unresolved.isEmpty) this  // no additional to resolve
        else iterate()()              // iterate over unresolved
      }

      /** fast equality check */
      override def equals(that: Any): Boolean = that match {
        case ResolutionPass(thoseResolved, thoseUnresolved) =>
          (resolved.length, unresolved.length) == (thoseResolved.length, thoseUnresolved.length)
        case _ => false
      }
    }

    /** repeatedly execute ResolutionPasses until all types resolved or no change occurs (cycle found) */
    def resolve(pass: ResolutionPass): LineItems = {
      pass.execute match {
        case ResolutionPass(allItems, Nil) => allItems
        case unchangedPass if (unchangedPass == pass) =>
          throw new Exception("Failed to resolve line items, circular dependency detected.")
        case intermediatePass => resolve(intermediatePass)
      }
    }

    resolve(ResolutionPass(unresolved = types))
  }


  //
  // utility members
  //
  def customerId: Long = _entity.customerId
  def accountId: Long = account map (_.id) getOrElse(0L)
  lazy val account: Option[Account] = customer.map(_.account)
  lazy val addresses = account.map(_.addresses).getOrElse(Nil)

  def fees: LineItems = lineItems(LineItemNature.Fee)
  def taxes: Seq[TaxLineItem] = lineItems(CodeType.Tax)
  def payments: LineItems = lineItems(LineItemNature.Payment)
  def balance: BalanceLineItem = lineItems(CodeType.Balance).head
  def subtotal: SubtotalLineItem = lineItems(CodeType.Subtotal).head
  def total: TotalLineItem = lineItems(CodeType.Total).head

  protected def summaryTypes: LineItemTypes = Seq(SubtotalLineItemType, TotalLineItemType, BalanceLineItemType)

}













//
// Companion Object
//
object Checkout {

  // Create
  def apply(types: LineItemTypes, maybeZipcode: Option[String], maybeCustomer: Option[Customer])
  : FreshCheckout = {
    val entity = CheckoutEntity(customerId = maybeCustomer.map(_.id).getOrElse(0L))
    new FreshCheckout(entity, types, maybeCustomer, maybeZipcode)
  }

  // Restore
  def apply(id: Long)(services: CheckoutServices = AppConfig.instance[CheckoutServices])
  : PersistedCheckout = { services.findById(id).get }


  //
  // Checkout failure cases
  //
  sealed abstract class CheckoutFailed(val failedCheckoutData: FailedCheckoutData)
  protected[checkout] trait FailedCheckoutWithCharge { def charge: Option[Charge] }

  case class CheckoutFailedCashTransactionResolutionError(
    checkout: Checkout,
    cashTransactionType: Option[CashTransactionLineItemType]
  ) extends CheckoutFailed(FailedCheckoutData(checkout, cashTransactionType))

  case class CheckoutFailedInsufficientInventory(
    checkout: Checkout,
    canceledTransactionItem: Option[CashTransactionLineItem],
    charge: Option[Charge]
  ) extends CheckoutFailed(
    FailedCheckoutData(checkout, canceledTransactionItem.map(_.itemType), canceledTransactionItem)
  ) with FailedCheckoutWithCharge

  case class CheckoutFailedCashTransactionMissing(checkout: Checkout)
    extends CheckoutFailed(FailedCheckoutData(checkout))

  case class CheckoutFailedError(
    checkout: Checkout,
    canceledTransactionItem: Option[CashTransactionLineItem],
    charge: Option[Charge]
  ) extends CheckoutFailed(
    FailedCheckoutData(checkout, canceledTransactionItem.map(_.itemType), canceledTransactionItem)
  ) with FailedCheckoutWithCharge
}

case class FailedCheckoutData(
  checkout: Checkout,
  cashTransactionLineItemTypes: Option[CashTransactionLineItemType] = None,
  cashTransactionLineItems: Option[CashTransactionLineItem] = None
)


