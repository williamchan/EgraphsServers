package controllers

import play.api._
import play.api.mvc._
import models.frontend.landing.CatalogStar
import helpers.DefaultImplicitTemplateParameters
import models.frontend.header.HeaderData

/**
 * Permutations of the landing page
 */
object Landing extends Controller with DefaultImplicitTemplateParameters {

  /**
   * Displays a permutation of the landing page with "catalog stars" counting
   * 0 <= n <= sampleStars.length
   **/
  def featuredStars(count: Int) = Action {
    Ok(views.html.frontend.landing(sampleStars.slice(0, count)))
  }

  /**
   * Displays a permutation of the page with gift certificate messaging up top. 
   **/

   def giftMessaging = Action {
    implicit val headerData = HeaderData(giftCertificateLink = Some("/gift"))
    val stars = sampleStars.zipWithIndex.map { case (star, index) =>
      star.copy(isFeatured = index < 8)
    }

    Ok(views.html.frontend.landing(stars)(headerData, defaultFooterData, authenticityToken)) 
   }

  def signupOn = Action {
    val stars = sampleStars.map(star => star.copy(inventoryRemaining = 0))
    Ok(views.html.frontend.landing(stars, signup=true))
  }

  def featuredStarsWithNoInventory() = Action {
    val stars = sampleStars.map(star => star.copy(inventoryRemaining = 0))
    Ok(views.html.frontend.landing(stars))
  }

  def singleCelebrity(publicName: String,
                       casualName: String,
                       isMale: Boolean) = Action {
    Ok(views.html.frontend.celebrity_landing(
      getStartedUrl = "/David-Price/photos",
      celebrityPublicName = publicName,
      celebrityCasualName = casualName,
      landingPageImageUrl = EgraphsAssets.at("images/ortiz_masthead.jpg").url,
      celebrityIsMale = isMale)
    )
  }

  private def makeSampleStar(id: Long, name: String, secondaryText: Option[String]): CatalogStar = {
    CatalogStar(
      id,
      name,
      secondaryText.getOrElse(""),
      organization = "",
      sampleImageUrl,
      sampleMarketplaceImageUrl,
      "/" + name,
      inventoryRemaining = 10,
      35,
      100
    )
  }

  private val sampleImageUrl = "http://placehold.it/440x157"
  private val sampleMarketplaceImageUrl = EgraphsAssets.at("images/660x350.gif").url

  private val sampleStars = Seq(
    makeSampleStar(1, "David Price", Some("Tampa Bay Rays")),
    makeSampleStar(2, "Josh Hamilton", Some("Texas Rangers")),
    makeSampleStar(3, "David Ortiz", Some("Boston Red Sox")),
    makeSampleStar(4, "Cal Ripken", Some("Baltimore Orioles")),
    makeSampleStar(5, "Lady Gaga", None),
    makeSampleStar(6, "Tom Petty", Some("Tom Petty and the Heartbreakers")),
    makeSampleStar(7, "Frodo Baggins", Some("Savior of Middle Earth")),
    makeSampleStar(8, "Sauron Himself", None)
  )
}

