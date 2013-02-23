package models.checkout

import com.google.inject.Inject
import models.{CouponStore, Coupon}
import models.checkout.checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}
import org.joda.money.{Money, CurrencyUnit}
import services.db.Schema
import services.AppConfig
import scalaz.Lens


//
// Services
//
case class CouponLineItemTypeServices @Inject() (
  schema: Schema,
  couponStore: CouponStore,
  lineItemStore: LineItemStore
)
  extends SavesAsLineItemTypeEntity[CouponLineItemType]
  with QueriesAsLineItemTypeEntity[CouponLineItemType]
{

  override def entityToModel(convert: LineItemTypeEntity) = {
    for (coupon <- couponStore.findByLineItemTypeId(convert.id).headOption)
      yield CouponLineItemType(convert, coupon)
  }

  /** primary point of entry into a CouponLineItemType, since the actual coupon is required */
  def findByCouponCode(code: String) = couponStore.findValid(code) map { coupon =>
    lazy val couponWithExistingEntity = coupon.lineItemTypeId flatMap { id =>
      assert(findEntityById(id) isDefined, "Coupon has invalid lineItemTypeId")
      findEntityById(id) map { entity => new CouponLineItemType(entity, coupon)}
    }

    lazy val couponWithNewEntity = {
      val savedEntity = insert(CouponLineItemType.baseEntityForCoupon(coupon))
      val updatedCoupon = coupon.withLineItemTypeId(savedEntity.id).save()
      new CouponLineItemType(savedEntity, updatedCoupon)
    }

    couponWithExistingEntity getOrElse couponWithNewEntity
  }
}


//
// Model
//
/**
 * For use of coupon in a checkout. For now it is assumed the coupon is active.
 * TODO(CE-13): Make sure coupon use can't be gamed (without making testing annoying).
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

  //
  // LineItemType members
  //
  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = {
    println("Resolved -- " + resolvedItems.map(_.codeType))
    println("Pending -- " + pendingResolution.map(_.codeType))

    val resolution = pendingResolution(CheckoutCodeType.Subtotal) match {
      case Nil => resolvedItems(CheckoutCodeType.Subtotal).headOption map { subtotal =>
        val discountAmount = coupon.calculateDiscount(subtotal.amount)
        Seq{ CouponLineItem(this, discountAmount.negated) }
      }
      case _ => None
    }

    println("Resolved coupon as resolution: " + resolution)

    resolution
  }

  //
  // LineItemTypeEntityLenses member
  //
  override protected val entityLens = Lens[CouponLineItemType, LineItemTypeEntity](
    get = _._entity,
    set = _ copy _
  )
}


//
//
// Companion
object CouponLineItemType {
  def nature = LineItemNature.Discount
  def codeType = CheckoutCodeType.Coupon

  def baseEntityForCoupon(coupon: Coupon) = {
    val desc = "For use of coupon #" + coupon.id
    LineItemTypeEntity(desc, nature, codeType)
  }
}
