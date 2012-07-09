package controllers

import play.mvc.Controller
import models.frontend.landing.FeaturedStar

/**
 * Permutations of the landing page
 */
object Landing extends Controller with DefaultHeaderAndFooterData {

  /**
   * Displays a permutation of the landing page with "featured stars" counting
   * 0 <= n <= sampleStars.length
   **/
  def featured_stars(count: Int) = {
    views.frontend.html.landing(featuredStars = sampleStars.slice(0, count))
  }

  def single_celebrity(publicName: String = "David Price",
                       casualName: String = "David",
                       isMale: Boolean = true) = {
    views.frontend.html.celebrity_landing(
      getStartedUrl = "/David-Price/photos",
      celebrityPublicName = publicName,
      celebrityCasualName = casualName,
      landingPageImageUrl = "/public/images/bikini_masthead.jpg",
      celebrityIsMale = isMale
    )
  }

  private def makeSampleStar(name: String, secondaryText: Option[String]): FeaturedStar = {
    import play.templates.JavaExtensions.slugify

    FeaturedStar(name, secondaryText, sampleImageUrl, "/" + slugify(name))
  }

  private val sampleImageUrl = "http://placehold.it/440x220"
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

