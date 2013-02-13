package models.checkout

import models.{Coupon, CouponStore}
import services.AppConfig
import services.db.{Schema, HasTransientServices}
import org.joda.money.{CurrencyUnit, Money}
import com.google.inject.Inject


case class CouponLineItemServices @Inject() (
  schema: Schema,
  couponStore: CouponStore,
  couponTypeServices: CouponLineItemTypeServices
) extends SavesAsLineItemEntity[CouponLineItem] {

}

case class CouponLineItem(
  _entity: LineItemEntity,
  _type: Option[CouponLineItemType] = None,
  @transient _services: CouponLineItemServices = AppConfig.instance[CouponLineItemServices]
)
	extends LineItem[Coupon]
	with HasLineItemEntity[CouponLineItem]
  with LineItemEntityGettersAndSetters[CouponLineItem]
  with SavesAsLineItemEntityThroughServices[CouponLineItem, CouponLineItemServices]
{

  override def toJson = ""

  override def itemType: CouponLineItemType = (_type orElse itemTypeById).get
  override lazy val domainObject: Coupon = (couponFromType orElse couponFromTypeId).get
  override def transact(checkout: Checkout) = {
    if (id > 0) { this.update() }
    else {
      domainObject.use(checkout.subtotal.amount).save()
      this.withCheckoutId(checkout.id).insert().copy(_type = None)
    }
  }

  private def couponFromType = _type map { _.coupon }
  private def couponFromTypeId = services.couponStore.findByLineItemTypeId(itemTypeId).headOption
  private def itemTypeById = services.couponTypeServices.findById(itemTypeId)

  override protected def entityLens = EntityLens( get = _._entity, set = _ copy _)
}


object CouponLineItem {
  def apply(itemType: CouponLineItemType, amount: Money) = {
    assert(itemType.id > 0)
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(amount)
    new CouponLineItem(entity, Some(itemType))
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    new CouponLineItem(entity)
  }
}
