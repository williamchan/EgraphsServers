package controllers

import play.mvc.Controller

import models.frontend.storefront.{PortraitOrientation, ChoosePhotoProductTile}

/**
 * Permutations of the Celebrity Storefront: Choose Photo, tiled view.
 */
object ChoosePhoto extends Controller {
  import frontend.formatting.services.MoneyFormatting.Conversions._

  def index = {
    views.frontend.html.celebrity_storefront_choose_photo_tiled()
  }

  private def sampleTile = {
    ChoosePhotoProductTile(
      name="2012 All-Star Game",
      price=BigDecimal(100.00).toMoney(),
      imageUrl="http://placehold.it/340x200",
      targetUrl="#",
      orientation=PortraitOrientation
    )
  }
}

