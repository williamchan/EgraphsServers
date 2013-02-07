package models.checkout

import com.google.inject.Inject
import models.{CashTransaction, CashTransactionStore}
import scalaz.Lens
import services.AppConfig
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import services.payment.Payment


//
// Services
//
case class CashTransactionLineItemServices @Inject() (
  schema: Schema,
  payment: Payment,
  cashTransactionStore: CashTransactionStore
) extends SavesAsLineItemEntity[CashTransactionLineItem] {

  override protected def modelWithNewEntity(txnItem: CashTransactionLineItem, newEntity: LineItemEntity) = {
    txnItem.entity.set(newEntity)
  }
}



//
// Model
//
/**
 * Once resolved from an item type, CashTransactionLineItem is used make charges for the amount of
 * holds the domain object representing the exchange of funds.
 *
 * When new/untransacted, initially takes the cash transaction, which may be incomplete since some
 * information is not known til the charge is made. When restored, only the entities are needed
 * since the CashTransaction can be queried from the db.
 *
 * @param _entity new or persisted LineItemEntity
 * @param _typeEntity entity of CashTransactionLineItemType resolved from
 * @param _maybeCashTransaction CashTransaction given by item type, otherwise None
 * @param services
 */
case class CashTransactionLineItem(
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  _maybeCashTransaction: Option[CashTransaction],
  services: CashTransactionLineItemServices = AppConfig.instance[CashTransactionLineItemServices]
) extends LineItem[CashTransaction] with HasLineItemEntity[CashTransactionLineItem]
  with LineItemEntityGettersAndSetters[CashTransactionLineItem]
  with CanInsertAndUpdateAsThroughServices[CashTransactionLineItem, LineItemEntity]
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
   * checkout, so it might not be an issue. Has not caused any noticable issues so far.
   *
   * TODO(CE-16): refactor, possibly apply to other line items
   */
  override lazy val itemType: CashTransactionLineItemType =
    CashTransactionLineItemType(_typeEntity, _entity)

  override def toJson = ""


  override lazy val domainObject: CashTransaction = _maybeCashTransaction.getOrElse {
    services.cashTransactionStore.findByLineItemId(id).getOrElse (
      throw new IllegalArgumentException("No cash transaction provided or found in database.")
    )
  }

  override def transact(checkout: Checkout) = {
    if (id > 0) { this }
    else {
      require( checkout.account.id > 0 )

      // note: type is not saved since the entities are singular
      val savedItem = this.withCheckoutId(checkout.id).insert()
      val savedCashTxn = domainObject.copy(
        accountId = checkout.account.id,
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
  def makeCharge(checkout: Checkout): CashTransactionLineItem = {
    require(amount.negated == domainObject.cash, "Line item amount and transaction amount are out of sync")

    if (checkout.balance.amount.isZero) { this }
    else {
      require(checkout.balance.amount == domainObject.cash, "Checkout balance and transaction amount are out of sync")
      require(checkout.id > 0, "Checkout with persisted entity required to make charge.")
      require(id <= 0, "Untransacted CashTransactionLineItem required to make charge.")
      require(_maybeCashTransaction.isDefined, "Required CashTransaction information is not present.")

      val txn = domainObject
      val charge = services.payment.charge(txn.cash, txn.stripeCardTokenId.get, "Checkout #" + checkout.id)
      val newTransaction = txn.copy(stripeChargeId = Some(charge.id))
      this.copy(_maybeCashTransaction = Some(newTransaction))
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


