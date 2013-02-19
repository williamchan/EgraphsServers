package models.checkout

import models.{Coupon, CouponStore}
import services.AppConfig
import services.db.{Schema, HasTransientServices}
import org.joda.money.{CurrencyUnit, Money}
import com.google.inject.Inject

//
// Services
//
case class CouponLineItemServices @Inject() (
  schema: Schema,
  couponStore: CouponStore,
  couponTypeServices: CouponLineItemTypeServices
) extends SavesAsLineItemEntity[CouponLineItem]



//
// Model
//
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

  //
  // LineItem members
  //
  override def itemType: CouponLineItemType = (_type orElse itemTypeById) getOrElse {
    throw new IllegalArgumentException("CouponLineItemType required.")
  }

  override lazy val domainObject: Coupon = (couponFromType orElse couponFromTypeId) getOrElse {
    throw new IllegalArgumentException("Coupon required.")
  }

  override def transact(checkout: Checkout) = {
    if (id > 0) { this.update() }
    else {
      domainObject.use(checkout.subtotal.amount).save()
      this.withCheckoutId(checkout.id).insert()
        .resetType
    }
  }

  override def toJson = jsonify(
    name = if (domainObject.isGiftCertificate) "Gift Certificate" else "Coupon",
    description = domainObject.name,
    id = Some(id)
  )

  //
  // LineItemEntityLenses member
  //
  override protected def entityLens = EntityLens( get = _._entity, set = _ copy _)

  //
  // Helpers
  //
  /** get coupon from itemType */
  private def couponFromType = _type map { _.coupon }

  /** get coupon from db by itemType id */
  private def couponFromTypeId = services.couponStore.findByLineItemTypeId(itemTypeId).headOption

  /** get itemType from db by its id */
  private def itemTypeById = services.couponTypeServices.findById(itemTypeId)

  /** clear _type parameter, used when _type is no longer current in transact */
  private def resetType = this.copy(_type = None)
}


//
// Companion
//
object CouponLineItem {
  //
  // Create
  //
  def apply(itemType: CouponLineItemType, amount: Money) = {
    assert(itemType.id > 0)
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(amount)
    new CouponLineItem(entity, Some(itemType))
  }

  //
  // Restore
  //
  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    new CouponLineItem(entity)
  }
}
