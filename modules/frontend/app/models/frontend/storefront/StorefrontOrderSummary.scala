package models.frontend.storefront

import org.joda.money.Money

/**
 * Order summary as seen on the Personalize page.
 * See [[views.frontend.html.celebrity_storefront_personalize.]]
 *
 * @param celebrityName name of the celebrity
 * @param productName name / title of the product
 * @param subtotal the cost of the product
 * @param shipping the cost of shipping the product
 * @param tax the tax amount on the product
 * @param total the sum of all costs
 */
case class StorefrontOrderSummary(
  celebrityName: String,
  productName: String,
  subtotal: Money,
  shipping: Option[Money],
  tax: Option[Money],
  total: Money
)
