package controllers

import play.mvc.Controller


/**
 * Permutations of the Checkout: Personalize.
 */
object Personalize extends Controller with DefaultHeaderAndFooterData {

  def index = {
    views.frontend.html.celebrity_storefront_personalize()
  }

}

