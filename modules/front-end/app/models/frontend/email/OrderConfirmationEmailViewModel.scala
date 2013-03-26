package models.frontend.email

import egraphs.playutils.Grammar

case class OrderConfirmationEmailViewModel(
  buyerName: String,
  buyerEmail: String,
  recipientName: String,
  recipientEmail: String,
  celebrityName: String,
  celebrityGrammar: Grammar,
  productName: String,
  orderDate: String,
  orderId: String,
  pricePaid: String,
  deliveredByDate: String,
  faqHowLongLink: String,
  messageToCelebrity: String,
  maybePrintOrderShippingAddress: Option[String]
) {
  def printOrderShippingAddress: String = maybePrintOrderShippingAddress getOrElse ("")
  def hasPrintOrder: Boolean = maybePrintOrderShippingAddress.isDefined
}