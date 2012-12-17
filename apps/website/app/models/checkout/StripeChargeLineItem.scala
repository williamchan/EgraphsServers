package models.checkout

import services.payment.StripeCharge
import org.joda.money.Money
import scalaz.Lens

case class StripeChargeLineItem(
  _entity: LineItemEntity,
  itemType: StripeChargeLineItemType,
  subItems: Seq[LineItem[_]] = Nil,
  _domainEntityId: Long = checkout.UnsavedEntity
) extends LineItem[StripeCharge] with HasLineItemEntity
  with LineItemEntityLenses[StripeChargeLineItem]
  with LineItemEntityGetters[StripeChargeLineItem]
  with LineItemEntitySetters[StripeChargeLineItem]
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
