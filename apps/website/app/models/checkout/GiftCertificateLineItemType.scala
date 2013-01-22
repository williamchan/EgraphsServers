package models.checkout

import models.GiftCertificate
import models.enums.{CodeType, LineItemNature}
import org.joda.money.Money
import services.AppConfig
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import scalaz.Lens
import com.google.inject.Inject


case class GiftCertificateLineItemType (
  _entity: LineItemTypeEntity,
  recipient: String,
  amount: Money,
  services: GiftCertificateLineItemTypeServices = AppConfig.instance[GiftCertificateLineItemTypeServices]
) extends LineItemType[GiftCertificate] with HasLineItemTypeEntity
  with LineItemTypeEntityGettersAndSetters[GiftCertificateLineItemType]
  with CanInsertAndUpdateAsThroughServices[GiftCertificateLineItemType, LineItemTypeEntity]
{

  override def toJson: String = {
    // TODO(SER-499): implement
    ""
  }

  override def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    val giftCertificate = GiftCertificate(recipient, amount)
    Some(Seq(GiftCertificateLineItem(this, giftCertificate)))
  }

  override def id = _entity.id

  override protected lazy val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )
}


object GiftCertificateLineItemType {
  /**
   * GiftCertificateLineItemType is for purchasing gift certificates. Using a gift certificate
   * is actually just using the coupon produced by the gift certificate line item. Hence, this
   * has a "Product" nature, rather than a discount nature.
   */
  val nature = LineItemNature.Product
  val codeType = CodeType.GiftCertificate

  // Create new LineItemType
  def apply(recipient: String, amountToBuy: Money): GiftCertificateLineItemType = {
    val entityDesc = entityDescription(recipient, amountToBuy)
    val entity = LineItemTypeEntity(entityDesc, nature, codeType)
    new GiftCertificateLineItemType(entity, recipient, amountToBuy)
  }

  // Restore a LineItemType
  def apply(lineItem: GiftCertificateLineItem) = {
    new GiftCertificateLineItemType(
      _entity = lineItem._typeEntity,
      recipient = lineItem.domainObject.recipient,
      amount = lineItem.amount
    )
  }

  private val entityDescBase = ""
  private def entityDescription(recipient: String, amount: Money) = {
    "Gift certificate for " ++ amount.toString ++ {
      if(recipient.isEmpty) ""
      else (" to " ++ recipient)
    }
  }
}








case class GiftCertificateLineItemTypeServices @Inject() (schema: Schema)
  extends SavesAsLineItemTypeEntity[GiftCertificateLineItemType]
{
  //
  // SavesAsLineItemTypeEntity members
  //
  override protected def modelWithNewEntity(certificate: GiftCertificateLineItemType, entity: LineItemTypeEntity) = {
    certificate.copy(_entity=entity)
  }
}