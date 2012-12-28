package models.checkout

import org.joda.money.Money
import scalaz.Lens
import services.AppConfig
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import com.google.inject.Inject
import models.{CashTransaction, CashTransactionStore}
import play.api.libs.json.Json


// TODO(SER-499): make class parametric in ChargeT, may need multiple services
// ex: case class ChargeLineItem[ChargeT <: Charge] extends LineItem[ChargeT]

case class CashTransactionLineItem(
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  makeCashTransaction: (CashTransactionLineItemServices) => Option[CashTransaction],
  services: CashTransactionLineItemServices = AppConfig.instance[CashTransactionLineItemServices]
) extends LineItem[CashTransaction] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[CashTransactionLineItem]
  with CanInsertAndUpdateAsThroughServices[CashTransactionLineItem, LineItemEntity]
{

  override lazy val itemType = CashTransactionLineItemType(_typeEntity, _entity)

  /** Should make a CashTransaction without actually charging customer */
  override lazy val domainObject: CashTransaction = {
    makeCashTransaction(services).getOrElse(
      throw new IllegalArgumentException("makeCashTransaction failed to produce CashTransaction object.")
    )
  }

  override def transact(newCheckoutId: Long): CashTransactionLineItem = {
    this  // TODO(SER-499): use actual StripePayment to charge, see payment handler
  }

  override def subItems: Seq[LineItem[_]] = Nil

  override def toJson: String = {
    ""    // TODO(SER-499): More Json
  }

  override protected lazy val entityLens = Lens[CashTransactionLineItem, LineItemEntity](
    get = charge => charge._entity,
    set = (charge, entity) => charge.copy(entity)
  )
}

object CashTransactionLineItem {

  def apply(itemType: CashTransactionLineItemType, amount: Money) = {
    val entity = LineItemEntity(amount, "Stripe charge for " + amount)

    val makeCashTransaction: (CashTransactionLineItemServices) => Option[CashTransaction] = {
      // TODO(SER-499): make the a CashTransaction without actually charging customer
      // NOTE(SER-499): make this an Option instead of function if this ends up being simple
      services => None
    }

    new CashTransactionLineItem(entity, itemType._entity, makeCashTransaction)
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    val makeCashTransaction = { (services: CashTransactionLineItemServices) =>
      services.cashTransactionStore.findById(entity._domainEntityId)
    }
    new CashTransactionLineItem( entity, typeEntity, makeCashTransaction )
  }



}




case class CashTransactionLineItemServices @Inject() (
  schema: Schema,
  cashTransactionStore: CashTransactionStore
) extends SavesAsLineItemEntity[CashTransactionLineItem] {
  // TODO(SER-499): determine what additional services are needed

  override protected def modelWithNewEntity(charge: CashTransactionLineItem, entity: LineItemEntity) = {
    charge.copy(_entity=entity)
  }
}