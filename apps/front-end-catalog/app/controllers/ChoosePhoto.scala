package controllers

import play.mvc.Controller

import models.frontend.storefront.{ChoosePhotoCelebrity, PortraitOrientation, ChoosePhotoProductTile}
import java.text.SimpleDateFormat

/**
 * Permutations of the Celebrity Storefront: Choose Photo, tiled view.
 */
object ChoosePhoto extends Controller {
  import frontend.formatting.MoneyFormatting.Conversions._

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  def index = {
    views.frontend.html.celebrity_storefront_choose_photo_tiled(
      sampleCeleb,
      List(sampleTile, sampleTile, sampleTile, sampleTile),
      List(icon)
    )
  }

  private def sampleCeleb = {
    ChoosePhotoCelebrity(
      name="David Price",
      profileUrl="http://placehold.it/80x100",
      category="Major League Baseball",
      categoryRole="Pitcher, Tampa Bay Rays",
      twitterUsername="davidprice14",
      quantityAvailable=10,
      deliveryDate=dateFormat.parse("2012-07-13")
    )
  }

  def carousel = {
    views.frontend.html.celebrity_storefront_choose_photo_carousel()
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

  private def icon = "http://placehold.it/100x50"
}

