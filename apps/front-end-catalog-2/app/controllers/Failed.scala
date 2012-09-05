package controllers

import play.mvc.Controller

object Failed extends Controller
with DefaultHeaderAndFooterData
with DefaultStorefrontBreadcrumbs
{
  def no_inventory = {
    views.html.frontend.celebrity_storefront_no_inventory(
      celebrityName = "{celebrity name}",
      productName = "{product name}"
    )
  }

  def creditcard_error = {
    views.html.frontend.celebrity_storefront_creditcard_error(
      celebrityName = "{celebrity name}",
      productName = "{product name}",
      creditCardMsg = "Your card was declined"
    )
  }

  def purchase_error = {
    views.html.frontend.celebrity_storefront_purchase_error(
      celebrityName = "{celebrity name}",
      productName = "{product name}"
    )
  }
}

