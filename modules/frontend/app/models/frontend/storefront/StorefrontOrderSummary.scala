package models.frontend.storefront

import org.joda.money.Money

case class StorefrontOrderSummary(
  celebrityName: String,
  productName: String,
  subtotal: Money,
  shipping: Option[Money],
  tax: Option[Money],
  total: Money
)
