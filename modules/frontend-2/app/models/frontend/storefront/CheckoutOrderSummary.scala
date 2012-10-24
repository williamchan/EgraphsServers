package models.frontend.storefront

import org.joda.money.Money

/**
 * Summary of the order as displated on the checkout screen.
 *
 * See [[views.html.frontend.celebrity_storefront_checkout]]
 *
 * @param celebrityName name of the celebrity
 * @param productName name/title of the product
 * @param recipientName name of the recipient, as provided in the Personalize form
 * @param messageText text of the message the buyer requested to be written on
 *     the egraph
 * @param basePrice the base price of the product
 * @param shipping cost of shipping
 * @param tax cost in tax
 * @param total total cost of the purchase.
 */
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
