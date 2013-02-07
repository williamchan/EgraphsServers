package models.checkout

import models.{Coupon, CouponStore}
import services.AppConfig
import services.db.{Schema, HasTransientServices}
import org.joda.money.{CurrencyUnit, Money}


case class CouponLineItemServices(
  schema: Schema,
  couponStore: CouponStore
) extends SavesAsLineItemEntity[CouponLineItem] {

}

case class CouponLineItem(
  _entity: LineItemEntity,
  _type: Option[CouponLineItemType] = None,
  @transient _services: CouponLineItemServices = AppConfig.instance[CouponLineItemServices]
) extends LineItem[Coupon] with HasLineItemEntity[CouponLineItem]
  with LineItemEntityGettersAndSetters[CouponLineItem]
  with SavesAsLineItemEntityThroughServices[CouponLineItem, CouponLineItemServices]
{

  override def itemType: CouponLineItemType = _type.get
  override def domainObject = (couponFromCode orElse couponFromTypeId).get
  override def transact(checkout: Checkout) = this
  override def toJson = ""

  private def couponFromCode = _type flatMap { couponType => services.couponStore.findByCode(couponType.couponCode).headOption }
  private def couponFromTypeId = services.couponStore.findByLineItemTypeId(1L).headOption

  override protected def entityLens = EntityLens( get = _._entity, set = _ copy _)
}


object CouponLineItem{
  def apply(itemType: CouponLineItemType, amount: Money) = {
    val entity = LineItemEntity(0,0,itemType.id).withAmount(amount)
    new CouponLineItem(entity, Some(itemType))
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    new CouponLineItem(entity)
  }
}
