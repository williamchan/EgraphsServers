package controllers

import play.mvc.Controller
import models.frontend.landing.CatalogStar

/**
 * Permutations of the landing page
 */
object Landing extends Controller with DefaultHeaderAndFooterData {

  /**
   * Displays a permutation of the landing page with "catalog stars" counting
   * 0 <= n <= sampleStars.length
   **/
  def featured_stars(count: Int) = {
    val stars = sampleStars.zipWithIndex.map { case (star, index) =>
      star.copy(isFeatured = index < count)
    }

    views.frontend.html.landing(stars)
  }

  def featured_stars_with_no_inventory() = {
    val stars = sampleStars.map(star => star.copy(hasInventoryRemaining = false))
    views.frontend.html.landing(stars)
  }

  def single_celebrity(publicName: String = "David Price",
                       casualName: String = "David",
                       isMale: Boolean = true) = {
    views.frontend.html.celebrity_landing(
      getStartedUrl = "/David-Price/photos",
      celebrityPublicName = publicName,
      celebrityCasualName = casualName,
      landingPageImageUrl = "/public/images/ortiz_masthead.jpg",
      celebrityIsMale = isMale
    )
  }

  private def makeSampleStar(name: String, secondaryText: Option[String]): CatalogStar = {
    import play.templates.JavaExtensions.slugify

    CatalogStar(
      name,
      secondaryText,
      sampleImageUrl,
      "/" + slugify(name),
      hasInventoryRemaining = true,
      isFeatured = false
    )
  }

  private val sampleImageUrl = "http://placehold.it/440x157"
  private val sampleStars = Seq(
    makeSampleStar("David Price", Some("Tampa Bay Rays")),
    makeSampleStar("Josh Hamilton", Some("Texas Rangers")),
    makeSampleStar("David Ortiz", Some("Boston Red Sox")),
    makeSampleStar("Cal Ripken", Some("Baltimore Orioles")),
    makeSampleStar("Lady Gaga", None),
    makeSampleStar("Tom Petty", Some("Tom Petty and the Heartbreakers")),
    makeSampleStar("Frodo Baggins", Some("Savior of Middle Earth")),
    makeSampleStar("Sauron Himself", None)
  )
}

