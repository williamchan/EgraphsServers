package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._
import com.google.inject.Inject


case class GiftCertificateLineItemType private (
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
    new GiftCertificateLineItemType(seedEntity(Some(recipient), Some(amountToBuy)), recipient, amountToBuy)
  }

  private[checkout] def seedEntity(maybeRecip: Option[String] = None, maybeAmount: Option[Money] = None) = {
    val desc = "Gift certificate" + maybeAmount.map(" for " + _).getOrElse("") + maybeRecip.map(" to " + _).getOrElse("")
    new LineItemTypeEntity(desc, nature, codeType)
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