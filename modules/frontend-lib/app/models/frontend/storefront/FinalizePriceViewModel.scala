package models.frontend.storefront

import org.joda.money.Money

/**
 * ViewModel for the pricing information as presented on the FinalizeOrder page.
 * See [[views.html.frontend.celebrity_storefront_finalize]]
 *
 * @param base the base price of the product.
 * @param physicalGood the price of ordering a physical good alongside the product.
 * @param tax the cost of taxes on the total.
 * @param total the total price of the order.
 */
case class FinalizePriceViewModel (
  base: Money,
  physicalGood: Option[Money],
  tax: Option[Money],
  total: Money
)
