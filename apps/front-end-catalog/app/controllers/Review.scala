package controllers

import play.mvc.Controller


/**
 * Permutations of the Checkout: Login.
 */
object Login extends Controller with DefaultHeaderAndFooterData {

  def index = {
    views.frontend.html.celebrity_storefront_login()
  }

}

