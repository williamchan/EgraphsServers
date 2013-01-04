package models.checkout

import org.joda.money.Money
import scalaz.Lens
import services.AppConfig
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import com.google.inject.Inject
import models.{CashTransaction, CashTransactionStore}
import play.api.libs.json.Json





// TODO(SER-499): move me
trait CashTransactionLineItem extends LineItem[CashTransaction] with HasLineItemEntity {
  protected[checkout] def abortTransaction() // refund charge

  override def subItems = Nil

  def withEntity(entity: LineItemEntity): CashTransactionLineItem

}

case class StripeCashTransactionLineItem (
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  _maybeCashTransaction: Option[CashTransaction],
  services: CashTransactionLineItemServices = AppConfig.instance[CashTransactionLineItemServices]
) extends CashTransactionLineItem
  with LineItemEntityGettersAndSetters[StripeCashTransactionLineItem]
  with CanInsertAndUpdateAsThroughServices[CashTransactionLineItem, LineItemEntity]
{

  //
  // LineItem
  //
  override def itemType: StripeCashTransactionLineItemType =
    StripeCashTransactionLineItemType(domainObject)

  override def toJson = ""

  override lazy val domainObject: CashTransaction = _maybeCashTransaction.getOrElse {
    services.cashTransactionStore.findByLineItemId(id).getOrElse(
      throw new IllegalArgumentException("No cash transaction provided or found in database.")
    )
  }

  override def transact(checkoutId: Long) = this

  
  
  //
  // CashTransactionLineItem
  //
  override protected[checkout] def abortTransaction() = { println("aborting and refunding charge") }

  override def withEntity(entity: LineItemEntity) = this.copy(entity)
  
  
  //
  // LineItemEntityLenses members
  //
  override lazy val entityLens = Lens[StripeCashTransactionLineItem, LineItemEntity](
    get = _._entity,
    set = _ withEntity _
  )
}

object StripeCashTransactionLineItem {
  //
  // Create
  //
  def apply(itemType: StripeCashTransactionLineItemType, transaction: CashTransaction) = {
    new StripeCashTransactionLineItem(
      LineItemEntity(transaction.cash, ""),
      itemType._entity,
      Some(transaction)
    )
  }

  //
  // Restore
  //
  def apply(entity: LineItemEntity, itemEntity: LineItemTypeEntity) = {
    new StripeCashTransactionLineItem(entity, itemEntity, None)
  }
}





case class CashTransactionLineItemServices @Inject() (
  schema: Schema,
  cashTransactionStore: CashTransactionStore
) extends SavesAsLineItemEntity[CashTransactionLineItem] {

  override protected def modelWithNewEntity(txnItem: CashTransactionLineItem, entity: LineItemEntity) = {
    txnItem.withEntity(entity)
  }
}