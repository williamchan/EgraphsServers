package controllers

import play.mvc.Controller

/* import models.frontend.storefront.{ } */

/**
 * Permutations of the Checkout: Personalize.
 */
object Checkout extends Controller {

  def index = {
    views.frontend.html.celebrity_storefront_checkout()
  }

}

