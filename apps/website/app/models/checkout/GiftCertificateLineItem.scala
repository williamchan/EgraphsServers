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
  _typeOrEntity: Either[GiftCertificateLineItemType, LineItemTypeEntity],
  _maybeCoupon: Option[Coupon] = None,
  services: GiftCertificateLineItemServices = AppConfig.instance[GiftCertificateLineItemServices]
) extends LineItem[Coupon] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[GiftCertificateLineItem]
  with CanInsertAndUpdateAsThroughServices[GiftCertificateLineItem, LineItemEntity]
{

  override def itemType: GiftCertificateLineItemType = _typeOrEntity match {
    case Left(itemType: GiftCertificateLineItemType) => itemType
    case Right(typeEntity: LineItemTypeEntity) => GiftCertificateLineItemType(
      _entity = typeEntity,
      amountToBuy = Money.of(CurrencyUnit.USD, _entity._amountInCurrency.bigDecimal)
    )
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
  override def transact(): GiftCertificateLineItem = {
    if (id <= 0) {
      require(checkoutId > 0, "Cannot transact without setting checkoutId.")

      /**
       * Save itemType first because it depends neither on a line item or domain object.
       * Then, save coupon, which only depends on itemType.
       * Finally, save the line item with the .
       */
      val savedType = itemType.insert()
      val savedCoupon = domainObject.copy(lineItemTypeId = savedType.id).save()
      val savedItem = withItemTypeId(savedType.id)
        .withDomainEntityId(savedCoupon.id)
        .insert()

      // return the saved item with its coupon's id stored
      savedItem.copy(_maybeCoupon = Some(savedCoupon))

    } else {
      this
    }
  }


  def withItemType(newType: GiftCertificateLineItemType) = {
    this.copy(_typeOrEntity = Left(newType)).withItemTypeId(newType.id)
  }


  override protected lazy val entityLens = Lens[GiftCertificateLineItem, LineItemEntity](
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )
}

object GiftCertificateLineItem {
  def apply(itemType: GiftCertificateLineItemType, coupon: Coupon) = {
    new GiftCertificateLineItem(
      new LineItemEntity(
        _itemTypeId = itemType.id,
        _amountInCurrency = itemType.amountToBuy.getAmount
      ),
      Left(itemType),
      Some(coupon)
    )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    new GiftCertificateLineItem(entity, Right(typeEntity))
  }
}







case class GiftCertificateLineItemServices @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore,
  lineItemTypeStore: LineItemTypeStore,
  couponStore: CouponStore
) extends SavesAsLineItemEntity[GiftCertificateLineItem] {
  // SaveAsLineItemEntity members
  override protected def modelWithNewEntity(certificate: GiftCertificateLineItem, entity: LineItemEntity) = {
    certificate.copy(_entity=entity)
  }
}