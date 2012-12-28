package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._
import com.google.inject.Inject
import play.api.libs.json.Json


case class GiftCertificateLineItemType (
  _entity: LineItemTypeEntity,
  recipient: String,
  amount: Money,
  services: GiftCertificateLineItemTypeServices = AppConfig.instance[GiftCertificateLineItemTypeServices]
) extends LineItemType[Coupon] with HasLineItemTypeEntity
  with LineItemTypeEntityGettersAndSetters[GiftCertificateLineItemType]
  with CanInsertAndUpdateAsThroughServices[GiftCertificateLineItemType, LineItemTypeEntity]
{

  override def toJson: String = {
    // TODO(SER-499): implement
    ""
  }

  override def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    val coupon = new Coupon(name = couponName, discountAmount = amount.getAmount)
      .withDiscountType(CouponDiscountType.Flat)
      .withCouponType(CouponType.GiftCertificate)
      .withUsageType(CouponUsageType.Prepaid)
    Seq(GiftCertificateLineItem(this, coupon))
  }

  override def id = _entity.id

  override protected lazy val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )

  // get recipient from JSON in entity description
  private val couponNameFormatString = "A gift certificate for %s"
  private def couponName: String = couponNameFormatString.format(recipient)
}


object GiftCertificateLineItemType {
  val nature = LineItemNature.Discount
  val codeType = CodeType.GiftCertificate
  val jsonAmountKey = "Amount"
  val jsonRecipientKey = "Recipient"

  // Create new LineItemType
  def apply(recipient: String, amountToBuy: Money): GiftCertificateLineItemType = {
    val entityDesc = entityDescriptionAsJsonString(amountToBuy, recipient)
    val entity = LineItemTypeEntity(entityDesc, nature, codeType)
    new GiftCertificateLineItemType(entity, recipient, amountToBuy)
  }

  // Restore a LineItemType
  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity) = {
    new GiftCertificateLineItemType(
      _entity = entity,
      recipient = recipientOptionFromEntity(entity)
        .getOrElse(throw new IllegalArgumentException("Could not parse recipient from entity.")),
      amount = Money.of(CurrencyUnit.USD, itemEntity._amountInCurrency.bigDecimal)
    )
  }

//  def amountOptionFromEntity(entity: LineItemTypeEntity): Option[BigDecimal] = {
//    (Json.parse(entity._desc) \ jsonAmountKey).asOpt[Double].map(BigDecimal(_))
//  }
  def recipientOptionFromEntity(entity: LineItemTypeEntity): Option[String] = {
    (Json.parse(entity._desc) \ jsonRecipientKey).asOpt[String]
  }

  private def entityDescriptionAsJsonString(amount: Money, recipient: String) = {
    val jsonDescriptionObject = Json.toJson( Map(
      // NOTE(SER-499): may need to wrap toJson in Seq
//      jsonAmountKey -> Json.toJson(amount.getAmount.doubleValue),
      jsonRecipientKey -> Json.toJson(recipient)
    ))
    Json.stringify(jsonDescriptionObject)
  }
}








case class GiftCertificateLineItemTypeServices @Inject() (schema: Schema)
  extends SavesAsLineItemTypeEntity[GiftCertificateLineItemType]
{

  // TODO(SER-499): query helpers

  //
  // SavesAsLineItemTypeEntity members
  //
  override protected def modelWithNewEntity(certificate: GiftCertificateLineItemType, entity: LineItemTypeEntity) = {
    certificate.copy(_entity=entity)
  }
}