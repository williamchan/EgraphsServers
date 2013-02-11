package models.checkout

import com.google.inject.Inject
import models.{CouponStore, Coupon}
import models.checkout.checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}
import org.joda.money.{Money, CurrencyUnit}
import services.db.Schema
import services.AppConfig
import scalaz.Lens



case class CouponLineItemTypeServices @Inject() (
  schema: Schema,
  couponStore: CouponStore,
  lineItemStore: LineItemStore
) extends SavesAsLineItemTypeEntity[CouponLineItemType] with QueriesAsLineItemTypeEntity[CouponLineItemType] {

  override def entityToModel(convert: LineItemTypeEntity) = {
    for (coupon <- couponStore.findByLineItemTypeId(convert.id).headOption)
      yield CouponLineItemType(convert, coupon)
  }

  def findByCouponCode(code: String) = couponStore.findValid(code) map { coupon =>
    def couponWithExistingEntity = coupon.lineItemTypeId flatMap { id =>
      assert(findEntityById(id) isDefined, "Coupon has invalid lineItemTypeId")
      findEntityById(id) map { entity => new CouponLineItemType(entity, coupon)}
    }

    def couponWithNewEntity = {
      val savedEntity = insert(CouponLineItemType.baseEntityForCoupon(coupon))
      val updatedCoupon = coupon.withLineItemTypeId(savedEntity.id).save()
      new CouponLineItemType(savedEntity, updatedCoupon)
    }

    couponWithExistingEntity getOrElse couponWithNewEntity
  }
}




/**
 * For using a coupon in a checkout. Assumes code is validated before being added (would be easy to validate here, but
 * seems like it should be a concern of the endpoint where it is added to the checkout).
 */
case class CouponLineItemType(
  _entity: LineItemTypeEntity,
  coupon: Coupon,
  @transient _services: CouponLineItemTypeServices = AppConfig.instance[CouponLineItemTypeServices]
)
  extends LineItemType[Coupon]
  with HasLineItemTypeEntity[CouponLineItemType]
  with LineItemTypeEntityGettersAndSetters[CouponLineItemType]
  with SavesAsLineItemTypeEntityThroughServices[CouponLineItemType, CouponLineItemTypeServices]
{

  override def toJson = ""
  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = Some{ Seq(
    CouponLineItem(this, Money.zero(CurrencyUnit.USD))
  )}


  override protected val entityLens = Lens[CouponLineItemType, LineItemTypeEntity](
    get = _._entity,
    set = _ copy _
  )
}


/**
 * todo: determine how coupon type entities will be managed
 *   -type per coupon?
 *     -a multi-use coupon can have a lineItemType, but not multiple line items (uses)
 */
object CouponLineItemType {
  def nature = LineItemNature.Discount
  def codeType = CheckoutCodeType.Coupon

  def baseEntityForCoupon(coupon: Coupon) = {
    val desc = "For use of coupon #" + coupon.id
    LineItemTypeEntity(desc, nature, codeType)
  }
}
