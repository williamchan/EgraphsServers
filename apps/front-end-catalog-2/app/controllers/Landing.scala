package controllers

import play.api._
import play.api.mvc._
import models.frontend.landing.CatalogStar
import helpers.DefaultImplicitTemplateParameters

/**
 * Permutations of the landing page
 */
object Landing extends Controller with DefaultImplicitTemplateParameters {

  /**
   * Displays a permutation of the landing page with "catalog stars" counting
   * 0 <= n <= sampleStars.length
   **/
  def featuredStars(count: Int) = Action {
    val stars = sampleStars.zipWithIndex.map { case (star, index) =>
      star.copy(isFeatured = index < count)
    }

    Ok(views.html.frontend.landing(stars))
  }

  def signupOn = Action {
    val stars = sampleStars.map(star => star.copy(hasInventoryRemaining = false))
    Ok(views.html.frontend.landing(stars, signup=true))
  }

  def featuredStarsWithNoInventory() = Action {
    val stars = sampleStars.map(star => star.copy(hasInventoryRemaining = false))
    Ok(views.html.frontend.landing(stars))
  }

  def singleCelebrity(publicName: String,
                       casualName: String,
                       isMale: Boolean) = Action {
    Ok(views.html.frontend.celebrity_landing(
      getStartedUrl = "/David-Price/photos",
      celebrityPublicName = publicName,
      celebrityCasualName = casualName,
      landingPageImageUrl = "images/ortiz_masthead.jpg",
      celebrityIsMale = isMale)
    )
  }

  private def makeSampleStar(name: String, secondaryText: Option[String]): CatalogStar = {
    import views.frontend.Utils.slugify

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

