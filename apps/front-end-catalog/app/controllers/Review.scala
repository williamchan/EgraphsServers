package controllers

import play.mvc.Controller


/**
 * Permutations of the Checkout: Login.
 */
object Login extends Controller {

  def index = {
    views.frontend.html.celebrity_storefront_login()
  }

}

