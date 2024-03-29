package models.checkout

import com.google.inject.Inject
import models.{CashTransaction, CashTransactionStore}
import scalaz.Lens
import services.AppConfig
import services.db.Schema
import services.payment.Payment
import com.stripe.exception.StripeException


//
// Services
//
case class CashTransactionLineItemServices @Inject() (
  schema: Schema,
  payment: Payment,
  cashTransactionStore: CashTransactionStore
) extends SavesAsLineItemEntity[CashTransactionLineItem]



//
// Model
//
/**
 * Once resolved from an item type, CashTransactionLineItem holds the domain object representing
 * the exchange of funds.
 *
 * When new/untransacted, initially takes the cash transaction, which may be incomplete since some
 * information is not known til the charge is made. When restored, only the entities are needed
 * since the CashTransaction can be queried from the db.
 *
 * @param _entity new or persisted LineItemEntity
 * @param _typeEntity entity of CashTransactionLineItemType resolved from
 * @param _maybeCashTransaction CashTransaction given by item type, otherwise None
 */
case class CashTransactionLineItem(
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  _maybeCashTransaction: Option[CashTransaction],
  @transient _services: CashTransactionLineItemServices = AppConfig.instance[CashTransactionLineItemServices]
)
  extends LineItem[CashTransaction]
  with HasLineItemEntity[CashTransactionLineItem]
  with LineItemEntityGettersAndSetters[CashTransactionLineItem]
  with SavesAsLineItemEntityThroughServices[CashTransactionLineItem, CashTransactionLineItemServices]
{

  //
  // LineItem members
  //
  /**
   * note: that between resolution from the item type and transaction, this creates an incorrect
   * CashTransactionLineItemType. Could instead take an Either[LineItemTypeEntity, CashTransactionLineItemType]
   * and return the contained item type if right and use the existing implementation if left.
   *
   * However, in this period of time, the original item type ought to be stored within the containing
   * checkout, so it might not be an issue. Has not caused any noticeable issues so far.
   *
   * TODO(CE-16): refactor, possibly apply to other line items
   */
  override lazy val itemType: CashTransactionLineItemType = CashTransactionLineItemType.restore(_typeEntity, _entity)

  override def toJson = jsonify("Cash Transaction", nature.name, Some(id))

  override lazy val domainObject: CashTransaction = (_maybeCashTransaction orElse getTxnFromDb) getOrElse (
    throw new IllegalArgumentException("No cash transaction provided or found in database.")
  )


  override def transact(checkout: Checkout) = {
    if (id > 0) { this.update() }
    else {
      // note: type is not saved since the entities are singular
      val savedItem = this.withCheckoutId(checkout.id).insert()
      val savedCashTxn = domainObject.copy(
        accountId = checkout.buyerAccount.id,
        lineItemId = Some(savedItem.id)
      ).save()

      savedItem.copy(_maybeCashTransaction = Some(savedCashTxn))
    }
  }


  /**
   * Charge customers card for the amount of the Checkout's balance
   *
   * @param checkout for which the charge is being made
   * @return CashTransactionLineItem with an update domainObject
   */
  def makeCharge(checkout: Checkout): Either[StripeException, CashTransactionLineItem] = {
    require(amount.negated == domainObject.cash, "Line item amount and transaction amount are out of sync")

    if (checkout.balance.amount.isZero) Right { this }
    else {
      require(checkout.balance.amount == domainObject.cash, "Checkout balance and transaction amount are out of sync")
      require(checkout.id > 0, "Checkout with persisted entity required to make charge.")
      require(id <= 0, "Untransacted CashTransactionLineItem required to make charge.")
      require(_maybeCashTransaction.isDefined, "Required CashTransaction information is not present.")

      val txn = domainObject
      val token = txn.stripeCardTokenId getOrElse (throw new IllegalArgumentException("Stripe token required."))
      val description = s"Checkout #${checkout.id} for ${checkout.buyerAccount.email}"

      try {
        val charge = services.payment.charge(txn.cash, token, description)
        val newTransaction = txn.copy(stripeChargeId = Some(charge.id))
        val itemWithNewTransaction = this.copy(_maybeCashTransaction = Some(newTransaction))

        Right(itemWithNewTransaction)

      } catch {
        case e: StripeException => Left(e)
        case e: Throwable => throw e
      }
    }
  }


  /** Refund the charge if made. Used in case of failed transaction in the Checkout. */
  protected[checkout] def abortTransaction() = {
    require(id <= 0, "Transaction has already completed. Update checkout with refund instead.")
    domainObject.stripeChargeId.map ( c => services.payment.refund (c) )
  }


  //
  // LineItemEntityLenses members
  //
  override protected lazy val entityLens = Lens[CashTransactionLineItem, LineItemEntity](
    get = txnItem => txnItem._entity,
    set = (txnItem, newEntity) => txnItem copy newEntity
  )

  def withPaymentService(newPayment: Payment) = this.copy(
    _services = _services.copy(payment = newPayment)
  )


  //
  // Helpers
  //
  private def getTxnFromDb = services.cashTransactionStore.findByLineItemId(id)

}



//
// Companion object
//
object CashTransactionLineItem {
  //
  // Create
  //
  def apply(itemType: CashTransactionLineItemType, transaction: CashTransaction) = {
    new CashTransactionLineItem(
      LineItemEntity(transaction.cash.negated, "", itemType.id),
      itemType._entity,
      Some(transaction)
    )
  }

  //
  // Restore
  //
  def apply(entity: LineItemEntity, itemEntity: LineItemTypeEntity) = {
    new CashTransactionLineItem(entity, itemEntity, None)
  }
}


