package models.checkout

import org.joda.money.Money
import scalaz.Lens
import services.AppConfig
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import com.google.inject.Inject
import models.{CashTransaction, CashTransactionStore}
import play.api.libs.json.Json




//
// Services
//
case class CashTransactionLineItemServices @Inject() (
  schema: Schema,
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
      val savedItem = this.withCheckoutId(checkout.id).insert()
      val savedCashTxn = domainObject.copy(
        accountId = checkout.account.id,
        lineItemId = Some(savedItem.id)
      ).save()

      savedItem.copy(_maybeCashTransaction = Some(savedCashTxn))

    } else {
      this
    }
  }


  def makeCharge(): CashTransactionLineItem = {
    // TODO(SER-499): charge through payment services and update cash transaction
    this
  }


  protected[checkout] def abortTransaction() = {
    // TODO(SER-499): implement charge refunding
    // [[[domainObject.stripeChargeId.map ( services.payment.refund _ )]]]
    println("aborting and refunding charge")
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
      LineItemEntity(transaction.cash, "", itemType.id),
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


