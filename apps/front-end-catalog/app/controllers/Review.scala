package controllers

import play.mvc.Controller


/**
 * Permutations of the Checkout: Review.
 */
object Review extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{

  def index = {
    views.frontend.html.celebrity_storefront_review()
  }

}

