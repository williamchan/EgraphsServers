package models.checkout

import _root_.exception.InsufficientInventoryException
import com.google.inject.Inject
import Conversions._
import exception.{DomainObjectNotFoundException, ItemTypeNotFoundException, MissingRequiredAddressException}
import java.sql.{Connection, Timestamp}
import play.api.libs.json._
import models._
import models.enums._
import play.api.libs.json.{JsValue, JsArray, JsNull}
import services.AppConfig
import services.db._
import services.payment.Charge
import services.config.ConfigFileProxy

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
  config: ConfigFileProxy,
  @CurrentTransaction currentTxnConnectionFactory: () => Connection
) extends InsertsAndUpdatesAsEntity[Checkout, CheckoutEntity] with SavesCreatedUpdated[CheckoutEntity] {
  import org.squeryl.PrimitiveTypeMode._

  override protected def table = schema.checkouts

  override def modelWithNewEntity(checkout: Checkout, entity: CheckoutEntity): Checkout = {
    checkout.withEntity(entity)
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
abstract class Checkout
  extends CanInsertAndUpdateEntityThroughTransientServices[Checkout, CheckoutEntity, CheckoutServices]
  with HasEntity[CheckoutEntity, Long]
{ this: Serializable =>
  import Checkout._

  //
  // Members
  //
  def id: Long
  def _entity: CheckoutEntity

  def buyerAccount: Account
  def buyerCustomer: Customer

  def recipientAccount: Option[Account]
  def recipientCustomer: Option[Customer]

  def payment: Option[CashTransactionLineItemType]
  def shippingAddress: Option[String]

  def save(): Checkout
  def zipcode: Option[String]
  def lineItems: LineItems
  def itemTypes: LineItemTypes
  def pendingItems: LineItems
  def pendingTypes: LineItemTypes
  protected def _dirty: Boolean     // simplifies transaction -- does nothing if clean

  /**
   * Types derived from zipcode and _addedTypes to be applied only to _addTypes (e.g. taxes, fees).
   * Needed to update things like taxes and fees, which are dependent on individual elements, when
   * adding new elements (ex: refunding taxes or including taxes on checkout edits).
   */
  protected lazy val _derivedTypes: LineItemTypes = Nil
//  if (!_dirty) { Nil } else {
//    // TODO(refunds): will probably want to add the refund transaction here
//    // TODO(fees): will want to add any fees we charge here
//
//    // TODO(taxes): remove the if to enable adding taxes based on billing zipcode
//    TaxLineItemType.getTaxesByZip(zipcode.getOrElse(TaxLineItemType.noZipcode))
//  }


  //
  // Methods
  //
  /** add additional types to this checkout -- for updating existing checkout */
  def withAdditionalTypes(newTypes: LineItemTypes): Checkout

  /** for services to return a persisted checkout as the correct type */
  def withEntity(savedEntity: CheckoutEntity): Checkout

  def toJson: JsValue = {
    def itemsToJson(items: LineItems) = items.foldLeft(JsArray(Nil)) { (acc, nextItem) =>
      nextItem.toJson match {
        case jsArray: JsArray => jsArray ++ acc
        case jsVal: JsValue => jsVal +: acc
      }
    }

    Json.toJson {
      LineItemNature.values map { nature =>
        (nature.name.toLowerCase, itemsToJson(lineItems(nature)))
      } toMap
    }
  }


  /**
   * Charges or credits buyer as needed through the information in the txnType, transacts all the
   * line items which have been added since the last transaction as well as the cash transaction
   * of the charge or credit to the buyer, and returns the resulting PersistedCheckout.
   *
   * @param txnType used to charge or credit buyer
   * @return failure, this if not _dirty, or the resulting PersistedCheckout
   */
  def transact(txnType: Option[CashTransactionLineItemType] = payment): FailureOrCheckout = {
    def nothingToTransact = pendingTypes.isEmpty
    def paymentMissing = !(balance.amount.isZero || txnType.isDefined)

    if (nothingToTransact) { Right(this) }
    else if (paymentMissing) { Left(CheckoutFailedCashTransactionMissing(this)) }
    else {
      // save checkout entity
      val savedCheckout = this.save()

      // resolve transaction item and make charge (or return error)
      val txnItem = savedCheckout.resolveCashTransactionAndCharge(txnType) match {
        case Right(txnItem) => txnItem
        case Left(failure) => return Left(failure)
      }

      /**
       * Transact items with a savepoint to fall back on if transacting line items fails. Note that charge must also
       * be refunded in error cases so that no unpaid items or payments for nonexistent item exist afterwards.
       */
      tryWithSavepoint {
        val allItems = txnItem.toSeq ++ pendingItems
        savedCheckout.transactItems(allItems)

        Right(services.findById(savedCheckout.id).get)

      } catchAndRollback {
        // TODO: this could be implemented more elegantly if LineItems return some Left error case directly
        case exc: Exception => Left {
          val refunded = txnItem flatMap (_.abortTransaction())
          exc match {
            case e: MissingRequiredAddressException => CheckoutFailedShippingAddressMissing(this, refunded, e.msg)
            case e: InsufficientInventoryException => CheckoutFailedInsufficientInventory(this, txnItem, refunded)
            case e: DomainObjectNotFoundException => CheckoutFailedDomainObjectNotFound(this, refunded, e.msg)
            case e: ItemTypeNotFoundException => CheckoutFailedItemTypeNotFound(this, refunded, e.msg)
            case e: Exception => CheckoutFailedError(this, txnItem, refunded, e)
          }
        }
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
          def merge(a: LineItemTypes, b: LineItemTypes) = if (a.size < b.size) {a ++ b} else {b ++ a}

          val otherTypes = merge(prevAttempted, unattempted)
          val itemsFromCurrent = current.lineItems(prevResolved, otherTypes)

          val resolvedNow = itemsFromCurrent.toSeq.flatten ++ prevResolved
          val attemptedNow = itemsFromCurrent match {
            case Some(_) => prevAttempted
            case None => current +: prevAttempted.toSeq
          }

          unattempted match {
            case next :: rest => iterate(prevResolved = resolvedNow)(
              prevAttempted = attemptedNow,
              current = next,
              unattempted = rest
            )
            case Nil => ResolutionPass(
              resolvedItems = resolvedNow,
              unresolvedTypes = attemptedNow
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

      protected def argLengths = (resolvedItems.size, unresolvedTypes.size)
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
  // helpers
  //
  /** get the line item of the given `CashTransactionLineItemType` as applied to this checkout and charge it */
  private def resolveCashTransactionAndCharge(maybeTxnType: Option[CashTransactionLineItemType])
  : Either[CheckoutFailed, Option[CashTransactionLineItem]] =
  {
    val txnItem = for (
      txnType <- maybeTxnType;
      txnItems <- txnType.lineItems(Seq(balance));
      txnItem <- txnItems.headOption
    ) yield txnItem

    (txnItem, balance.amount.isZero) match {
      case (Some(item), false) => Right { Some(item.makeCharge(this)) }
      case (None, false) => Left{ CheckoutFailedCashTransactionResolutionError(this, maybeTxnType) }
      case (_, true)  => Right(None)
    }
  }

  /**
   * Transact the given line items
   *
   * @param items to be transacted
   * @return transacted line items
   */
  private def transactItems(items: LineItems) = {
    require(id > 0, "Checkout must be transacted before items.")
    items map ( item => item.transact(this))
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
  def fees: LineItems = lineItems(LineItemNature.Fee)
  def taxes: Seq[TaxLineItem] = lineItems(CheckoutCodeType.Tax).seq.toSeq
  def coupons: Seq[CouponLineItem] = lineItems(CheckoutCodeType.Coupon)
  def payments: LineItems = lineItems(LineItemNature.Payment)

  /**
   * Note that these methods are as safe as the lineItems method since the corresponding item types should be included
   * by the implementation where appropriate. So, if these fail, it should be in lineItems, rather than from calling
   * head on an empty seq.
   */
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
  def create(types: LineItemTypes) = FreshCheckout(_itemTypes = types)

  // Restore
  def restore(id: Long)(implicit services: CheckoutServices = AppConfig.instance[CheckoutServices])
  : Option[PersistedCheckout] = {
    services.findById(id)
  }

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
    FailedCheckoutData(checkout, canceledTransactionItem.map(_.itemType), canceledTransactionItem, charge)
  ) with FailedCheckoutWithCharge

  case class CheckoutFailedCashTransactionMissing(checkout: Checkout) extends CheckoutFailed(FailedCheckoutData(checkout))

  case class CheckoutFailedItemTypeNotFound(
    checkout: Checkout,
    charge: Option[Charge],
    msg: String
  ) extends CheckoutFailed(FailedCheckoutData(checkout, charge = charge)) with FailedCheckoutWithCharge

  case class CheckoutFailedShippingAddressMissing(
    checkout: Checkout,
    charge: Option[Charge],
    msg: String
  ) extends CheckoutFailed(FailedCheckoutData(checkout, charge = charge)) with FailedCheckoutWithCharge

  case class CheckoutFailedDomainObjectNotFound(
    checkout: Checkout,
    charge: Option[Charge],
    msg: String
  ) extends CheckoutFailed(FailedCheckoutData(checkout, charge = charge)) with FailedCheckoutWithCharge

  case class CheckoutFailedError(
    checkout: Checkout,
    canceledTransactionItem: Option[CashTransactionLineItem],
    charge: Option[Charge],
    exception: Exception
  ) extends CheckoutFailed(
    FailedCheckoutData(checkout, canceledTransactionItem.map(_.itemType), canceledTransactionItem, charge)
  ) with FailedCheckoutWithCharge
}

case class FailedCheckoutData(
  checkout: Checkout,
  cashTransactionLineItemType: Option[CashTransactionLineItemType] = None,
  cashTransactionLineItem: Option[CashTransactionLineItem] = None,
  charge: Option[Charge] = None
)
