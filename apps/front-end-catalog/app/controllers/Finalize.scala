package controllers

import play.mvc.Controller


/**
 * Permutations of the Checkout: Finalize.
 */
object Finalize extends Controller with DefaultHeaderAndFooterData {

  def index = {
    views.frontend.html.celebrity_storefront_finalize()
  }

}

