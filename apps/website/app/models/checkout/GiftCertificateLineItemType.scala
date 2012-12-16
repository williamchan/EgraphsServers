package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.db.Schema
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._



case class GiftCertificateLineItemType (
  _entity: LineItemTypeEntity,
  recipient: String = "",
  amountToBuy: Money = Money.zero(CurrencyUnit.USD)
) extends LineItemType[Coupon] with HasLineItemTypeEntity
  with LineItemTypeEntityLenses[GiftCertificateLineItemType]
  with LineItemTypeEntityGetters[GiftCertificateLineItemType]
  with LineItemTypeEntitySetters[GiftCertificateLineItemType]
{
  // TODO(SER-499): implement
  override def toJson: String = ""

  override def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    Seq(GiftCertificateLineItem.fromItemType(this))
  }

  //
  // LineItemTypeEntityLenses members
  //
  override protected lazy val entityLens = GiftCertificateLineItemType.entityLens
}


object GiftCertificateLineItemType {
  /**
   * Place holder value for restored GiftCertificateLineItems since they
   * have no need to restore their LineItemTypes
   */
  val Unset = new GiftCertificateLineItemType(
    new LineItemTypeEntity(nature = nature, codeType = codeType)
  )

  // for convenience
  val nature = LineItemNature.Discount
  val codeType = CodeType.GiftCertificate

  val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )

  def apply(recipient: String, amountToBuy: Money): GiftCertificateLineItemType = {
    new GiftCertificateLineItemType(
      entityWithDescription(description(recipient, amountToBuy)),
      recipient,
      amountToBuy
    )
  }

  def description(recip: String, amount: Money) = "Gift certificate for " + amount + " to " + recip
  def entityWithDescription(desc: String = "Gift certificate") =
    new LineItemTypeEntity(0, desc, nature, codeType)
}
