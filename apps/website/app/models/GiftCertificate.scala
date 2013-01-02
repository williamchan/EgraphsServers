package models

// TODO(SER-499): add GiftCertificate table to schema and evolutions

import checkout.{GiftCertificateLineItem, LineItemEntity, LineItemStore}
import enums.{CouponUsageType, CouponType, CouponDiscountType}
import services.{Time, MemberLens, AppConfig}
import org.squeryl.{Query, KeyedEntity}
import org.joda.money.{CurrencyUnit, Money}
import com.google.inject.Inject
import java.sql.Timestamp
import services.db._




case class GiftCertificateServices @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore
) extends InsertsAndUpdatesAsEntity[GiftCertificate, GiftCertificateEntity]
  with SavesCreatedUpdated[GiftCertificateEntity]
{

  override protected def table = schema.giftCertificates

  override protected def modelWithNewEntity(certificate: GiftCertificate, entity: GiftCertificateEntity) = {
    certificate.entity.set(entity)
  }

  override def withCreatedUpdated(entity: GiftCertificateEntity, created: Timestamp, updated: Timestamp) = {
    entity.copy(created = created, updated = updated)
  }
}



case class GiftCertificate protected (
  _entity: GiftCertificateEntity,
  _coupon: Coupon,
  services: GiftCertificateServices = AppConfig.instance[GiftCertificateServices]
) extends HasEntity[GiftCertificateEntity, Long]
  with CanInsertAndUpdateAsThroughServices[GiftCertificate, GiftCertificateEntity]
{
  import MemberLens.Conversions._

  def saveFromLineItem(item: GiftCertificateLineItem) = {
    this.typeId.set(item.itemType.id)
      .itemId.set(item.id)
      .save()
  }

  def save(): GiftCertificate = {
    // TODO(SER-499): require itemId and typeId > 0
    this.coupon.set(_coupon.save()).insert()
  }

  def code: String = _coupon.code
  def recipient = _entity.recipient
  def balance: Money = Money.of(CurrencyUnit.USD, _coupon.discountAmount.bigDecimal)
  def purchaseAmount: Money = {
    /**
     * Get the original amount from the line item, but if the line item doesn't exist,
     * then the balance should be the full purchase amount since this gift certificate,
     * its coupon, and line item haven't been persisted yet.
     */
    maybeItemEntity.map( itemEntity =>
      Money.of(CurrencyUnit.USD, itemEntity._amountInCurrency.bigDecimal)
    ).getOrElse(balance)
  }


  private lazy val maybeItemEntity = services.lineItemStore.findEntityById(itemId.get)


  //
  // Lenses
  //
  lazy val entity = MemberLens[GiftCertificate, GiftCertificateEntity](this)(_entity, copy(_))
  lazy val coupon = MemberLens[GiftCertificate, Coupon](this)( _coupon,
    newCoupon => this.couponId.set(newCoupon.id).copy(_coupon = newCoupon)
  )

  lazy val couponId = entityIdLens(entity.couponId, id => entity.set(entity.couponId.set(id)))
  lazy val itemId = entityIdLens(entity.lineItemId, id => entity.set(entity.lineItemId.set(id)))
  lazy val typeId = entityIdLens(entity.lineItemTypeId, id => entity.set(entity.lineItemTypeId.set(id)))

  private def entityIdLens = MemberLens[GiftCertificate, Long](this)(_, _)
}


object GiftCertificate {
  def apply(recipient: String, amount: Money) = {
    val entity = GiftCertificateEntity(recipient = recipient)

    // TODO(SER-499): coupon should have some Discount-natured line item type
    val coupon = new Coupon(name = couponName(recipient), discountAmount = amount.getAmount)
      .withDiscountType(CouponDiscountType.Flat)
      .withCouponType(CouponType.GiftCertificate)
      .withUsageType(CouponUsageType.Prepaid)

    new GiftCertificate(entity, coupon)
  }


  def apply(entity: GiftCertificateEntity, coupon: Coupon) = {
    require(coupon.id == entity.couponId.get, "Coupon does not match gift certificate entity.")
    new GiftCertificate(entity, coupon)
  }


  private def couponName(recipient: String): String = {
    "Gift certificate" ++ {
      if (recipient.isEmpty) ""
      else (" for " ++ recipient)
    }
  }
}

case class GiftCertificateEntity (
  _couponId: Long = 0L,
  _lineItemId: Long = 0L,
  _lineItemTypeId: Long = 0L,
  recipient: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  override def id = _couponId
  override def unapplied = GiftCertificateEntity.unapply(this)

  def couponId = idLens(_couponId, id => copy(_couponId = id))
  def lineItemId = idLens(_lineItemId, id => copy(_lineItemId = id))
  def lineItemTypeId = idLens(_lineItemTypeId, id => copy(_lineItemTypeId = id))

  private def idLens = MemberLens[GiftCertificateEntity, Long](this)(_,_)
}





class GiftCertificateStore @Inject() (
  schema: Schema,
  couponStore: CouponStore,
  lineItemStore: LineItemStore
) {
  import org.squeryl.PrimitiveTypeMode._

  protected def table = schema.giftCertificates

  def findByLineItemId(id: Long): Option[GiftCertificate] = {
    for (
      entity <- table.where(entity => entity._lineItemId === id).headOption;
      coupon <- couponStore.findById(entity.couponId())
    ) yield GiftCertificate(entity, coupon)
  }
}