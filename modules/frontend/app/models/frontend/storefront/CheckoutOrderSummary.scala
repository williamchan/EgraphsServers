package models.frontend.storefront

import org.joda.money.Money


case class CheckoutOrderSummary(
  celebrityName: String,
  productName: String,
  recipientName: String,
  messageText: String,
  basePrice: Money,
  shipping: Option[Money],
  tax: Option[Money],
  total: Money
)

