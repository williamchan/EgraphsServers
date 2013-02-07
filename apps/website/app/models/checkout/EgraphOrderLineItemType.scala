package models.checkout

import models.Order
import org.joda.money.{CurrencyUnit, Money}


// TODO(CE-13): implement actual EgraphOrderLineItemType
case class EgraphOrderLineItemType(
  productId: Long,
  recipientName: String,
  isGift: Boolean,
  desiredText: Option[String],
  messageToCeleb: Option[String],
  framedPrint: Boolean
) extends LineItemType[Order] {

  import models.checkout.checkout.Conversions._
  import models.enums.{LineItemNature, CheckoutCodeType}

  override def toJson = ""
  override def description = ""
  override def id = 0L
  override def codeType = CheckoutCodeType.Balance // wrong code type
  override def nature = LineItemNature.Product
  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = Some{ Seq(
    EgraphOrderLineItem(this, Money.zero(CurrencyUnit.USD))
  ) }

  def order = Order(
    productId = productId,
    recipientName = recipientName,
    messageToCelebrity = messageToCeleb,
    requestedMessage = desiredText
  )
}
