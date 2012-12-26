package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._
import com.google.inject.Inject


case class GiftCertificateLineItemType (
  _entity: LineItemTypeEntity,
  recipient: String = "",
  amountToBuy: Money = Money.zero(CurrencyUnit.USD),
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
    val coupon = new Coupon(
      name = GiftCertificateLineItemType.couponName(this),  // TODO(SER-499): clean couponName use up
      discountAmount = amountToBuy.getAmount,
      lineItemTypeId = id)
    .withCouponType( CouponType.GiftCertificate )
    .withDiscountType( CouponDiscountType.Flat )
    .withUsageType( CouponUsageType.Prepaid )

    Seq(GiftCertificateLineItem(this, coupon))
  }

  override def id = _entity.id

  override protected lazy val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )
}


object GiftCertificateLineItemType {
  val nature = LineItemNature.Discount
  val codeType = CodeType.GiftCertificate

  def apply(
    recipient: String,
    amountToBuy: Money
  ): GiftCertificateLineItemType = {
    new GiftCertificateLineItemType(
      seedEntity(Some(recipient), Some(amountToBuy)),
      recipient, amountToBuy)
  }

  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity) = {
    new GiftCertificateLineItemType(
      _entity = entity,
      // skipping recipient because it's not handled well...
      amountToBuy = Money.of(CurrencyUnit.USD, itemEntity._amountInCurrency.bigDecimal)
    )
  }

  private val basicDescription = "Gift certificate"

  private[checkout] def seedEntity(implicit maybeRecipient: Option[String] = None, maybeAmount: Option[Money] = None) = {
    val desc = description(maybeRecipient, maybeAmount)
    LineItemTypeEntity(desc, nature, codeType)
  }

  private def description(implicit maybeRecipient: Option[String] = None, maybeAmount: Option[Money] = None) = {
    basicDescription ++
      maybeAmount.map(amount => " for " ++ amount.toString).getOrElse("") ++
      maybeRecipient.map(recip => " to " ++ recip).getOrElse("")
  }

  //
  // Coupon helpers
  //
  protected val couponNameFormatString = "A gift certificate for %s"
  protected def couponName(itemType: GiftCertificateLineItemType): String = {
    couponNameFormatString.format(itemType.recipient)
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