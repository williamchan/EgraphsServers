package controllers

import play.mvc.Controller

/* import models.frontend.storefront.{ } */

/**
 * Permutations of the Checkout: Review.
 */
object Review extends Controller {

  def index = {
    views.frontend.html.celebrity_storefront_review()
  }

}

