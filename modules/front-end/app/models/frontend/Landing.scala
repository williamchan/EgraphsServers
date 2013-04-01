package models.frontend.landing

import models.frontend.masthead.{VideoPlayerViewModel, CallToActionViewModel}
import controllers.EgraphsAssets

/**
 * A "Catalog Star" as seen in the celebrity catalog and in the Featured Stars on the
 * home page
 *
 * @param name the star's name, e.g. "David Ortiz"
 * @param secondaryText pithy explanatory text, e.g. "Pitcher, Boston Red Sox"
 * @param imageUrl URL to the thumbnail image.
 * @param storefrontUrl URL to the celebrity's storefront.
 * @param inventoryRemaining inventory left to buy
 */
case class CatalogStar(
  id: Long, 
  name: String,
  secondaryText: String,
  organization: String,
  imageUrl: String,
  marketplaceImageUrl: String,
  storefrontUrl: String,
  inventoryRemaining: Int,
  minPrice: Int,
  maxPrice: Int
) {
  def hasInventoryRemaining: Boolean = (inventoryRemaining > 0)
}

case class LandingMasthead(
  id:Long = 0,
  name: String = "default",
  headline: String,
  subtitle: Option[String] = None,
  landingPageImageUrl: String,
  callToActionViewModel : CallToActionViewModel
)

object DefaultLanding {
  val masthead = LandingMasthead(
    headline = "Connect With Your Star",
    landingPageImageUrl = EgraphsAssets.at("images/landing-masthead.jpg").url,
    callToActionViewModel = VideoPlayerViewModel("View Video", "/stars")
  )
}