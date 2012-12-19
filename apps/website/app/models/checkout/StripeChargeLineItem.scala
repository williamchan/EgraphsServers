package models.checkout

import services.payment.StripeCharge
import org.joda.money.Money
import scalaz.Lens
import services.AppConfig
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import com.google.inject.Inject

case class StripeChargeLineItem(
  _entity: LineItemEntity,
  itemType: StripeChargeLineItemType,
  subItems: Seq[LineItem[_]] = Nil,
  _domainEntityId: Long = 0,
  services: StripeChargeLineItemServices = AppConfig.instance[StripeChargeLineItemServices]
) extends LineItem[StripeCharge] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[StripeChargeLineItem]
  with CanInsertAndUpdateAsThroughServices[StripeChargeLineItem, LineItemEntity]
{
  override def toJson: String = {
    // TODO(SER-499): More Json
    ""
  }


  override def domainObject: StripeCharge = {
    // TODO(SER-499): figure out how these StripeCharges work
    null
  }


  override def transact: StripeChargeLineItem = {
    // TODO(SER-499): use actual StripePayment to charge, see payment handler
    this
  }


  override protected lazy val entityLens = Lens[StripeChargeLineItem, LineItemEntity](
    get = charge => charge._entity,
    set = (charge, entity) => charge.copy(entity)
  )
}

object StripeChargeLineItem {
  def apply(itemType: StripeChargeLineItemType, amount: Money) = {
    new StripeChargeLineItem(new LineItemEntity(amount), itemType)
  }
}






case class StripeChargeLineItemServices @Inject() (
  schema: Schema
) extends SavesAsLineItemEntity[StripeChargeLineItem] {
  // TODO(SER-499): determine what additional services are needed

  override protected def modelWithNewEntity(charge: StripeChargeLineItem, entity: LineItemEntity) = {
    charge.copy(_entity=entity)
  }
}