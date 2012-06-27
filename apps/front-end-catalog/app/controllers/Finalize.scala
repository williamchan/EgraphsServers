package controllers

import play.mvc.Controller


/**
 * Permutations of the Checkout: Finalize.
 */
object Finalize extends Controller {

  def index = {
    views.frontend.html.celebrity_storefront_finalize()
  }

}

