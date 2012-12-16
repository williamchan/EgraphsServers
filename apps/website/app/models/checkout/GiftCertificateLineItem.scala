package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.Money
import services.AppConfig
import services.db.Schema
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._


/**
 * NOTE(SER-499): as it stands, a line item's state is rather different depending on whether it is unpersisted (_domainEntityId = 0), persisted, or restored (itemType = unset). This is a sign of deficiencies in the design and may lead to errors since it's difficult to think of each possible state when writing these line items. Would really like to make this easier to reason about/treat each state uniformly.
 *
 * TODO(SER-499): possibly make itemType an option, since it would be convenient to leave it as None when restoring (unless restoring is so infrequent it just creates noise for little benefit?)
 *
 * @param _entity
 * @param itemType - line item type corresponding to this gift certificate
 * @param subItems - items that depend upon or relate strongly to this gift certificate
 * @param _domainEntityId
 */
case class GiftCertificateLineItem (
  _entity: LineItemEntity = new LineItemEntity(),
  itemType: GiftCertificateLineItemType = GiftCertificateLineItemType.Unset,
  subItems: Seq[LineItem[_]] = Nil,
  _domainEntityId: Long = GiftCertificateLineItem.Unpersisted
) extends LineItem[Coupon] with HasLineItemEntity
  with LineItemEntityLenses[GiftCertificateLineItem]
  with LineItemEntityGetters[GiftCertificateLineItem]
  with LineItemEntitySetters[GiftCertificateLineItem]
{

  // TODO(SER-499): implement once api nailed down
  override def toJson: String = ""

  override def domainObject: Coupon = {
    if (_domainEntityId == GiftCertificateLineItem.Unpersisted) {
      new Coupon(
        name = GiftCertificateLineItem.couponName(itemType),
        discountAmount = amount.getAmount,
        lineItemTypeId = itemType.id
      ).withCouponType( CouponType.GiftCertificate
      ).withDiscountType( CouponDiscountType.Flat
      ).withUsageType( CouponUsageType.Prepaid )
    } else {
      // TODO(SER-499): query db for coupon
      null // to be a query
    }
  }

  /**
   * Presumes that checkoutId is set; persists unsaved instances
   * @return persisted line item
   */
  override def transact: GiftCertificateLineItem = {
    require(checkoutId > 0, "Cannot transact without setting checkoutId.")

    if (_domainEntityId == GiftCertificateLineItem.Unpersisted) {
      import GiftCertificateLineItemTypeServices.Conversions._
      import GiftCertificateLineItemServices.Conversions._

      /**
       * Save itemType first because it depends neither on a line item or domain object.
       * Then, save line item with the resulting itemType.
       * Finally, save the resulting line item's domain object.
       */
      val savedType = itemType.create()
      val savedItem = withItemType(savedType).create()
      val savedCoupon = savedItem.domainObject.save()

      // return the saved item with its coupon's id stored
      savedItem.copy(_domainEntityId = savedCoupon.id)

    } else {
      this
    }
  }


  override protected def entityLens = Lens[GiftCertificateLineItem, LineItemEntity](
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )

  override def withCheckoutId(newCheckoutId: Long): GiftCertificateLineItem = {
    this.withCheckoutId(newCheckoutId)
  }

  def withItemType(newType: GiftCertificateLineItemType) = {
    this.copy(itemType = newType).withItemTypeId(newType.id)
  }
}

object GiftCertificateLineItem {
  def fromItemType(itemType: GiftCertificateLineItemType) = {
    new GiftCertificateLineItem(
      new LineItemEntity(
        _itemTypeId = itemType.id,
        _amountInCurrency = itemType.amountToBuy.getAmount,
        notes = couponName(itemType)
      ),
      itemType
    )
  }

  // constant for unpersisted domain entities' ids
  protected val Unpersisted = 0

  //
  // Coupon helpers
  //
  protected val couponNameFormatString = "A gift certificate for %s"
  protected def couponName(itemType: GiftCertificateLineItemType): String = {
    couponNameFormatString.format(itemType.recipient)
  }
}