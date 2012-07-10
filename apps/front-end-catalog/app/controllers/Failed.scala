package controllers

import play.mvc.Controller

object Failed extends Controller
with DefaultHeaderAndFooterData
with DefaultStorefrontBreadcrumbs
{
  def index = {
    views.frontend.html.celebrity_storefront_no_inventory(
      celebrityName = "{celebrity name}",
      productName = "{product name}"
    )
  }
}

