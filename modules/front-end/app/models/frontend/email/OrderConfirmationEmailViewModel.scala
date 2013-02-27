package models.frontend.email

import egraphs.playutils.Gender

case class OrderConfirmationEmailViewModel(
  buyerName: String,
  buyerEmail: String,
  recipientName: String,
  recipientEmail: String,
  celebrityName: String,
  celebrityGender: Gender.EnumVal,
  productName: String,
  orderDate: String,
  orderId: String,
  pricePaid: String,
  deliveredByDate: String,
  faqHowLongLink: String,
  maybePrintOrderShippingAddress: Option[String]
) {
  def printOrderShippingAddress: String = maybePrintOrderShippingAddress getOrElse ("")
  def hasPrintOrder: Boolean = maybePrintOrderShippingAddress.isDefined
}