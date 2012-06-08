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

  private val sampleImageUrl = "http://placehold.it/440x220"
  private val sampleStars = Seq(
    FeaturedStar("David Price", "Tampa Bay Rays", sampleImageUrl),
    FeaturedStar("Josh Hamilton", "Texas Rangers", sampleImageUrl),
    FeaturedStar("David Ortiz", "Boston Red Sox", sampleImageUrl),
    FeaturedStar("Cal Ripken", "Baltimore Orioles", sampleImageUrl),
    FeaturedStar("Lady Gaga", "Musician", sampleImageUrl),
    FeaturedStar("Tom Petty", "Tom Petty and the Heartbreakers", sampleImageUrl),
    FeaturedStar("Frodo Baggins", "Savior of Middle Earth", sampleImageUrl),
    FeaturedStar("Sauron Himself", "Lord of the Rings", sampleImageUrl)
  )

}

