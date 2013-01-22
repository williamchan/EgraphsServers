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
case class CashTransactionLineItem(
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  _maybeCashTransaction: Option[CashTransaction],
  services: CashTransactionLineItemServices = AppConfig.instance[CashTransactionLineItemServices]
) extends LineItem[CashTransaction] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[CashTransactionLineItem]
  with CanInsertAndUpdateAsThroughServices[CashTransactionLineItem, LineItemEntity]
{

  //
  // LineItem members
  //
  override lazy val itemType: CashTransactionLineItemType =
    CashTransactionLineItemType(_typeEntity, domainObject)

  override def toJson = ""
  override def subItems = Nil

  override lazy val domainObject: CashTransaction = _maybeCashTransaction.getOrElse {
    services.cashTransactionStore.findByLineItemId(id).getOrElse (
      throw new IllegalArgumentException("No cash transaction provided or found in database.")
    )
  }

  override def transact(checkout: Checkout) = {
    // TODO(SER-499): persist entity and type (type entity as of now should be singleton)
    if (id <= 0) {
      require( checkout.accountId > 0 )

      val savedItem = this.withCheckoutId(checkout.id).insert()
      val savedCashTxn = domainObject.copy(
        accountId = checkout.accountId,
        lineItemId = Some(savedItem.id)
      ).save()

      savedItem.copy(_maybeCashTransaction = Some(savedCashTxn))

    } else {
      this
    }
  }


  def makeCharge(checkout: Checkout): CashTransactionLineItem = {
    require(amount.negated isEqual domainObject.cash, "Line item amount and transaction amount are out of sync")

    if (amount.isZero) {
      // DEBUG
      println("not charging because amount is zero")
      // END DEBUG

      this

    } else {
      require(checkout.id > 0, "Checkout with persisted entity required to make charge.")
      require(id <= 0, "Untransacted CashTransactionLineItem required to make charge.")
      require(_maybeCashTransaction.isDefined, "Required CashTransaction information is not present.")

      // DEBUG
      println("making charge")
      // END DEBUG

      val txn = domainObject
      val charge = services.payment.charge(txn.cash, txn.stripeCardTokenId.get, "Checkout #" + checkout.id)
      val newTransaction = txn.copy(stripeChargeId = Some(charge.id))
      this.copy(_maybeCashTransaction = Some(newTransaction))
    }
  }


  protected[checkout] def abortTransaction() = {
    require(id <= 0, "Transaction has already completed. Update checkout with refund instead.")

    // DEBUG
    println("aborting and refunding charge")
    // END DEBUG

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


