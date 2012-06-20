package controllers

import play.mvc.Controller
import models.frontend.landing.FeaturedStar

/**
 * Permutations of the Celebrity Storefront: Choose Photo, tiled view.
 */
object ChoosePhoto extends Controller {

  def index = {
    views.frontend.html.celebrity_storefront_choose_photo_tiled()
  }
}

