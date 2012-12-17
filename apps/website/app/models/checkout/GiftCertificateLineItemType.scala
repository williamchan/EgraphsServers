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

  override def toJson: String = {
    // TODO(SER-499): implement
    ""
  }

  override def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    Seq(GiftCertificateLineItem.fromItemType(this))
  }


  override protected lazy val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )
}


object GiftCertificateLineItemType {
  val nature = LineItemNature.Discount
  val codeType = CodeType.GiftCertificate

  def apply(recipient: String, amountToBuy: Money): GiftCertificateLineItemType = {
    new GiftCertificateLineItemType(
      entityWithDescription(description(recipient, amountToBuy)),
      recipient,
      amountToBuy
    )
  }

  // TODO(SER-499): Unmagicify
  def description(recip: String, amount: Money) = "Gift certificate for " + amount + " to " + recip
  def entityWithDescription(desc: String = "Gift certificate") =
    new LineItemTypeEntity(desc, nature, codeType)
}
