package models.checkout

import models.{GiftCertificateStore, GiftCertificate}
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._
import com.google.inject.Inject



case class GiftCertificateLineItem (
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  _maybeGiftCertificate: Option[GiftCertificate] = None,
  services: GiftCertificateLineItemServices = AppConfig.instance[GiftCertificateLineItemServices]
) extends LineItem[GiftCertificate] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[GiftCertificateLineItem]
  with CanInsertAndUpdateAsThroughServices[GiftCertificateLineItem, LineItemEntity]
{
//  require(_maybeGiftCertificate.isDefined || _entity._domainEntityId > 0)

  override def itemType = GiftCertificateLineItemType(this)

  override def subItems: Seq[LineItem[_]] = Nil

  override def toJson: String = {
    // TODO(SER-499): implement once api nailed down
    ""
  }


  override def domainObject: GiftCertificate = {
    _maybeGiftCertificate.getOrElse {
      services.giftCertificateStore.findByLineItemId(id).headOption.getOrElse {
        throw new IllegalArgumentException("No associated GiftCertificate exists for this Gift Certificate.")
      }
    }
  }


  /**
   * Presumes that checkoutId is set; persists unsaved instances
   * @return persisted line item
   */
  override def transact(checkout: Checkout): GiftCertificateLineItem = {
    if (id <= 0) {
      /**
       * Save itemType first because it depends neither on a line item or domain object.
       * Then, save giftCertificate, which only depends on itemType.
       * Finally, save the line item with the saved giftCertificate and type.
       */
      val savedType = itemType.insert()
      val savedItem = this.withCheckoutId(checkout.id)
        .withItemType(savedType)
        .insert()
      val savedGiftCertificate = domainObject.saveWithLineItem(savedItem)

      savedItem.withGiftCertificate(savedGiftCertificate)
    } else {
      this
    }
  }

  def withGiftCertificate(giftCertificate: GiftCertificate) = {
    this.copy(
      _maybeGiftCertificate = Some(giftCertificate.itemId.set(id))
    )
  }


  def withItemType(newType: GiftCertificateLineItemType) = {
    this.withItemTypeId(newType.id).copy(_typeEntity = newType._entity)
  }


  //
  // EntityLenses members
  //
  override protected lazy val entityLens = Lens[GiftCertificateLineItem, LineItemEntity](
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )
}



object GiftCertificateLineItem {
  // Creating
  def apply(itemType: GiftCertificateLineItemType, giftCertificate: GiftCertificate) = {
    new GiftCertificateLineItem(
      _entity = new LineItemEntity(
          _itemTypeId = itemType.id,
          _amountInCurrency = itemType.amount.getAmount
        ),
      _typeEntity = itemType._entity,
      _maybeGiftCertificate = Some(giftCertificate)
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
  giftCertificateStore: GiftCertificateStore
) extends SavesAsLineItemEntity[GiftCertificateLineItem] {
  // SaveAsLineItemEntity members
  override protected def modelWithNewEntity(certificate: GiftCertificateLineItem, entity: LineItemEntity) = {
    certificate.copy(_entity=entity)
  }
}