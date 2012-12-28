package models.checkout

import models.{CouponStore, Coupon}
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._
import com.google.inject.Inject


/**
 * NOTE(SER-499): as it stands, a line item's state is rather different depending on whether it is unpersisted (_domainEntityId = 0), persisted, or restored (itemType = unset). This is a sign of deficiencies in the design and may lead to errors since it's difficult to think of each possible state when writing these line items. Would really like to make this easier to reason about/treat each state uniformly.
 *
 * TODO(SER-499): possibly make itemType an option, since it would be convenient to leave it as None when restoring (unless restoring is so infrequent it just creates noise for little benefit?)
 */
case class GiftCertificateLineItem (
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  _maybeCoupon: Option[Coupon] = None,
  services: GiftCertificateLineItemServices = AppConfig.instance[GiftCertificateLineItemServices]
) extends LineItem[Coupon] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[GiftCertificateLineItem]
  with CanInsertAndUpdateAsThroughServices[GiftCertificateLineItem, LineItemEntity]
{
  require(_maybeCoupon.isDefined || _entity._domainEntityId > 0)

  override def itemType: GiftCertificateLineItemType = {
    GiftCertificateLineItemType(_typeEntity, _entity)
  }

  override def subItems: Seq[LineItem[_]] = Nil

  override def toJson: String = {
    // TODO(SER-499): implement once api nailed down
    ""
  }


  override def domainObject: Coupon = {
    _maybeCoupon.getOrElse {
      services.couponStore.findByLineItemTypeId(itemType.id).headOption.getOrElse {
        throw new IllegalArgumentException("No associated Coupon exists for this Gift Certificate.")
      }
    }
  }


  /**
   * Presumes that checkoutId is set; persists unsaved instances
   * @return persisted line item
   */
  override def transact(newCheckoutId: Long): GiftCertificateLineItem = {
    if (id <= 0) {
      /**
       * Save itemType first because it depends neither on a line item or domain object.
       * Then, save coupon, which only depends on itemType.
       * Finally, save the line item with the saved coupon and type.
       */
      val savedType = itemType.insert()
      val savedCoupon = domainObject.copy(lineItemTypeId = savedType.id).save()
      this.withCheckoutId(newCheckoutId)
        .withItemType(savedType)
        .withCoupon(savedCoupon)
        .insert()

    } else {
      this
    }
  }

  def withCoupon(coupon: Coupon) = {
    this.withDomainEntityId(coupon.id).copy(_maybeCoupon = Some(coupon))
  }

  def withItemType(newType: GiftCertificateLineItemType) = {
    this.withItemTypeId(newType.id).copy(_typeEntity = newType._entity)
  }


  override protected lazy val entityLens = Lens[GiftCertificateLineItem, LineItemEntity](
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )
}

object GiftCertificateLineItem {
  // Creating
  def apply(itemType: GiftCertificateLineItemType, coupon: Coupon) = {
    new GiftCertificateLineItem(
      _entity = new LineItemEntity(
          _itemTypeId = itemType.id,
          _amountInCurrency = itemType.amount.getAmount
        ),
      _typeEntity = itemType._entity,
      _maybeCoupon = Some(coupon)
    )
  }

  // Restoring
  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    new GiftCertificateLineItem(entity, typeEntity, None)
  }
}







case class GiftCertificateLineItemServices @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore,
  couponStore: CouponStore
) extends SavesAsLineItemEntity[GiftCertificateLineItem] {
  // SaveAsLineItemEntity members
  override protected def modelWithNewEntity(certificate: GiftCertificateLineItem, entity: LineItemEntity) = {
    certificate.copy(_entity=entity)
  }
}