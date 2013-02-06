package models.frontend.email

case class OrderConfirmationViewModel(
  buyerName: String,
  buyerEmail: String,
  recipientName: String,
  recipientEmail: String,
  celebrityName: String,
  productName: String,
  orderDate: String,
  orderId: String,
  pricePaid: String,
  deliveredByDate: String,
  faqHowLongLink: String,
  hasPrintOrder: Boolean
)