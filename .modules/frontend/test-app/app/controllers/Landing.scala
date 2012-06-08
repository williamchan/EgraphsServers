package controllers

import play.mvc.Controller
import models.frontend.landing.FeaturedStar

/**
 * Permutations of the landing page
 */
object Landing extends Controller {
  def featured_stars(count: Int) = {
    views.frontend.html.landing(sampleStars.slice(0, count))
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

