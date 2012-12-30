package models

// TODO(SER-499): add GiftCertificate table to schema and evolutions

import checkout.{LineItemEntity, LineItemStore}
import enums.{CouponUsageType, CouponType, CouponDiscountType}
import services.{Time, MemberLens, AppConfig}
import org.squeryl.{Query, KeyedEntity}
import org.joda.money.{CurrencyUnit, Money}
import com.google.inject.Inject
import java.sql.Timestamp
import services.db.{KeyedCaseClass, SavesWithLongKey, Schema}



case class GiftCertificateServices @Inject() (
  store: GiftCertificateStore,
  lineItemStore: LineItemStore
)


case class GiftCertificate protected (
  _entity: GiftCertificateEntity,
  _coupon: Coupon,
  services: GiftCertificateServices = AppConfig.instance[GiftCertificateServices] // ???
) /* extends ... */ {

  def save(): GiftCertificate = {
    // TODO(SER-499): whatever shopkeeping required to keep entity and coupon in sync
    // save coupon, set couponId, etc
    entity.set(services.store.save(_entity))
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
  lazy val coupon = MemberLens[GiftCertificate, Coupon](this)(
    (cert) => cert._coupon,
    (cert, newCoupon) => cert.copy(_coupon = newCoupon)
  )

  lazy val entity = MemberLens[GiftCertificate, GiftCertificateEntity](this)(
    (cert) => cert._entity,
    (cert, newEntity) => cert.copy(_entity = newEntity)
  )


  lazy val couponId = entityIdLens(
    (cert) => _entity.couponId.get,
    (cert, id) => entity.set(_entity.couponId.set(id))
  )

  lazy val itemId = entityIdLens(
    (cert) => _entity.lineItemId.get,
    (cert, id) => entity.set(_entity.lineItemId.set(id))
  )

  lazy val typeId = entityIdLens(
    (cert) => _entity.lineItemTypeId.get,
    (cert, id) => entity.set(_entity.lineItemTypeId.set(id))
  )

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

  def couponId = idLens(_._couponId, (entity, id) => entity.copy(_couponId = id))
  def lineItemId = idLens(_._lineItemId, (entity, id) => entity.copy(_lineItemId = id))
  def lineItemTypeId = idLens(_._lineItemTypeId, (entity, id) => entity.copy(_lineItemTypeId = id))

  private def idLens = MemberLens[GiftCertificateEntity, Long](this)(_,_)
}




class GiftCertificateStore @Inject() (
  schema: Schema,
  couponStore: CouponStore
) extends SavesWithLongKey[GiftCertificateEntity]
  with SavesCreatedUpdated[GiftCertificateEntity]
{
  import org.squeryl.PrimitiveTypeMode._

  override def table = schema.giftCertificates
  override def withCreatedUpdated(entity: GiftCertificateEntity, created: Timestamp, updated: Timestamp) = {
    entity.copy(created = created, updated = updated)
  }

  def findByLineItemId(id: Long): Option[GiftCertificate] = {

    val maybeEntity = from(table){ entity =>
      where(entity._lineItemTypeId === id) select(entity)
    }.headOption

    for (entity <- maybeEntity; coupon <- couponStore.findById(entity.couponId()))
      yield { GiftCertificate(entity, coupon) }
  }
}