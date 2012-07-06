package controllers

import play.mvc.Controller

/**
 * Permutations of the Checkout: Order Complete.
 */
object Complete extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{
  def index = {
    views.frontend.html.celebrity_storefront_complete()
  }

}
