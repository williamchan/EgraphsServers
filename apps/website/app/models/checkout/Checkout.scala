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


  /**
   * Charges or credits customer as needed through the information in the txnType, transacts all the
   * line items which have been added since the last transaction as well as the cash transaction
   * of the charge or credit to the customer, and returns the resulting PersistedCheckout.
   *
   * @param txnType used to charge or credit customer
   * @return failure, this if not _dirty, or the resulting PersistedCheckout
   */
  def transact(txnType: Option[CashTransactionLineItemType]): FailureOrCheckout = {
    def nothingToTransact = pendingTypes.isEmpty
    def paymentMissing = !(balance.amount.isZero || txnType.isDefined)
    def customerMissing = !customer.isDefined

    if (nothingToTransact) { Right(this) }
    else if (paymentMissing) { Left(CheckoutFailedCashTransactionMissing(this)) }
    else if (customerMissing) { Left(CheckoutFailedCustomerMissing(this, txnType)) }
    else {
      // save checkout
      val savedCheckout = this.save()

      // resolve transaction item and make charge (or return error)
      val txnItem = resolveCashTransaction(txnType) match {
        case Some(item) => Some( item.makeCharge(savedCheckout) )
        case _ => return Left{ CheckoutFailedCashTransactionResolutionError(this, txnType) }
      }

      def refundedCharge = txnItem flatMap (_.abortTransaction())

      /**
       * Transact items with a savepoint to fall back on if transacting line items fails.
       * Note that charge must also be refunded in error cases so that no unpaid items or
       * payments for nonexistent item exist afterwards.
       */
      tryWithSavepoint {
        val allItems = txnItem.toSeq ++ pendingItems
        transactItemsForCheckout(allItems)(savedCheckout)

        Right(PersistedCheckout(savedCheckout))

      } catchAndRollback {
        case e: InsufficientInventoryException =>
          Left { CheckoutFailedInsufficientInventory(this, txnItem, refundedCharge) }

        case e: Exception =>
          Left { CheckoutFailedError(this, txnItem, refundedCharge) }
      }
    }
  }


  /**
   * Resolves the given LineItemTypes into LineItems. Optionally takes some line items as a context
   * for resolving the given types. For example:
   *
   * {{{
   *   // supposing fooItem is a transacted LineItem in our checkout
   *   val refundFoo = RefundLineItemType(fooItem.id)
   *
   *   // generate the set of lineItems including the refund for fooItem
   *   val itemsWithRefundedFoo = resolveItems(Seq(refundFoo), lineItems)
   * }}}
   *
   * Note that resolveTypes doesn't take care of managing Summaries (total, subtotal, balance); it
   * only handles resolution, no higher level semantics. (To update summaries, you would want to
   * resolve the summaries' types again with the preexisting items having the old summaries filtered
   * out.)
   *
   * todo(ce-16): filter out summaries from preexistingItems before resolving when summary types are
   * being resolved if managing summaries becomes problematic. Perhaps add a way for items to
   * declare that there should be at most one in a checkout.
   *
   * @param types `LineItemType`s to be resolved
   * @param preexistingItems already resolved `LineItem`s
   * @return newly resolved items with preexisting items
   */
  protected def resolveTypes(types: LineItemTypes, preexistingItems: LineItems = Nil): LineItems = {
    /**
     * Handles the logic of a single pass over the unresolved types. Iterates over unresolved types,
     * calling their `lineItems` method, and returns a ResolutionPass with the newly resolved items
     * and remaining unresolved types.
     */
    case class ResolutionPass(resolvedItems: LineItems = preexistingItems, unresolvedTypes: LineItemTypes) {

      /** execute a pass over the unresolved items, return resulting ResolutionPass */
      def execute: ResolutionPass = {

        /**
         * The point of this is to reduce the amount of O(n) operations from a naive approach to
         * get the average case less than O(n^2).
         *
         * @param prevResolved accumulator of items resolved over iteration
         * @param prevAttempted attempted but unresolved types
         * @param current currently attempting to resolve
         * @param unattempted yet to be tried
         * @return - ResolutionPass of resulting resolved items and unresolved types
         */
        def iterate(prevResolved: LineItems = resolvedItems)(
          prevAttempted: LineItemTypes = Nil,
          current: LineItemType[_] = unresolvedTypes.head,
          unattempted: LineItemTypes = unresolvedTypes.tail
        ): ResolutionPass = {
          def merge(a: LineItemTypes, b: LineItemTypes) = if (a.length < b.length) {a ++ b} else {b ++ a}

          val otherTypes = merge(prevAttempted, unattempted)
          val itemsFromCurrent = current.lineItems(prevResolved, otherTypes)

          val resolvedNow = itemsFromCurrent.getOrElse(Nil) ++ prevResolved
          val attemptedNow =
            if (itemsFromCurrent isDefined) prevAttempted
            else current +: prevAttempted

          unattempted match {
            case next :: rest => iterate(prevResolved = resolvedNow)(
              prevAttempted = attemptedNow,
              current = next,
              unattempted = rest
            )
            case Nil => ResolutionPass(
              resolvedItems = resolvedNow,
              unresolvedTypes = prevAttempted
            )
          }
        }

        if (unresolvedTypes.isEmpty) this  // nothing to iterate over
        else iterate()()                   // start iteration
      }

      /** fast equality check */
      override def equals(that: Any): Boolean = that match {
        case thatPass: ResolutionPass => this.argLengths == thatPass.argLengths
        case _ => false
      }

      protected def argLengths = (resolvedItems.length, unresolvedTypes.length)
    }

    /**
     * Handles the logic of repeated iteration of unresolved types; executes ResolutionPasses until
     * all types resolved or no change occurs (cycle found).
     */
    def resolve(pass: ResolutionPass): LineItems = {
      pass.execute match {
        case ResolutionPass(allItems, Nil) => allItems
        case unchangedPass if (unchangedPass == pass) =>
          throw new Exception("Failed to resolve line items, circular dependency detected.")
        case intermediatePass => resolve(intermediatePass)
      }
    }

    // initiate resolution
    resolve(ResolutionPass(unresolvedTypes = types))
  }


  //
  // Helper methods
  //
  def save(): Checkout = if (id > 0) { this.update() } else { this.insert() }

  /** get the line item of the given `CashTransactionLineItemType` as applied to this checkout */
  private def resolveCashTransaction(txnType: Option[CashTransactionLineItemType])
  : Option[CashTransactionLineItem] = {
    txnType.flatMap { _.lineItems( Seq(balance) ) }
      .flatMap { _.headOption }
  }

  /**
   * Transact the given line items against the given checkout
   *
   * @param items to be transacted
   * @param checkout a saved checkout, passed to each item's `transact`
   * @return transacted line items
   */
  private def transactItemsForCheckout(items: LineItems)(checkout: Checkout) = {
    items map (_.transact(checkout))
  }

  /**
   * try/catch that sets a savepoint before try and rollsback if exception thrown
   * Use:
   * {{{
   *   tryWithSavepoint {
   *     // do some db stuff and other things with side effects
   *   } catchAndRollback {
   *     case e: Exception =>
  *        // deal with side effects, rollback handled for you
   *   }
   * }}}
   */
  private def tryWithSavepoint(tryBlock: => FailureOrCheckout) = new {
    def catchAndRollback (catchBlock: PartialFunction[Throwable, Left[CheckoutFailed, Nothing]]) = {
      val conn = services.currentTxnConnectionFactory()
      val savepoint = conn.setSavepoint()
      try { tryBlock } catch {
        case e: Exception =>
          conn.rollback(savepoint)
          catchBlock(e)
      }
    }
  }





  //
  // utility members
  //
  def customerId: Long = _entity.customerId
  def accountId: Long = account map (_.id) getOrElse(0L)
  lazy val account: Option[Account] = customer.map(_.account)
  lazy val addresses = account.map(_.addresses).getOrElse(Nil)

  def fees: LineItems = lineItems(LineItemNature.Fee)
  def taxes: Seq[TaxLineItem] = lineItems(CheckoutCodeType.Tax)
  def payments: LineItems = lineItems(LineItemNature.Payment)
  def balance: BalanceLineItem = lineItems(CheckoutCodeType.Balance).head
  def subtotal: SubtotalLineItem = lineItems(CheckoutCodeType.Subtotal).head
  def total: TotalLineItem = lineItems(CheckoutCodeType.Total).head

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


  case class CheckoutFailedCustomerMissing(
    checkout: Checkout,
    cashTransactionType: Option[CashTransactionLineItemType]
  ) extends CheckoutFailed(FailedCheckoutData(checkout, cashTransactionType))

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


