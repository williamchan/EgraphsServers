package controllers

import play.api._
import play.api.mvc._
import models.frontend.landing.{CatalogStar}
import models.frontend.marketplace.{CategoryValueViewModel, VerticalViewModel}
import helpers.DefaultImplicitTemplateParameters
import models.frontend.header.HeaderData
import egraphs.playutils.Gender

/**
 * Permutations of the landing page
 */
object Landing extends Controller with DefaultImplicitTemplateParameters {

  /**
   * Displays a permutation of the landing page with "catalog stars" counting
   * 0 <= n <= sampleStars.length
   **/
  def featuredStars(count: Int) = Action {
    Ok(views.html.frontend.landing(sampleStars.slice(0, count),verticalViewModels = Marketplace.landingVerticalSet, marketplaceRoute  = marketplaceRoute))
  }


  /**
   * Displays a permutation of the page with gift certificate messaging up top. 
   **/

   def giftMessaging = Action {
    implicit val headerData = HeaderData(giftCertificateLink = Some("/gift"))
    Ok(views.html.frontend.landing(sampleStars, Marketplace.landingVerticalSet, signup = false, marketplaceRoute)(headerData, defaultFooterData, authenticityToken))
   }

  def signupOn = Action {
    val stars = sampleStars.map(star => star.copy(inventoryRemaining = 0))
    Ok(views.html.frontend.landing(sampleStars, Marketplace.landingVerticalSet, signup = true, marketplaceRoute))
  }

  def featuredStarsWithNoInventory() = Action {
    val stars = sampleStars.map(star => star.copy(inventoryRemaining = 0))
    Ok(views.html.frontend.landing(stars, Marketplace.landingVerticalSet, signup = false, marketplaceRoute))
  }

  def singleCelebrity(publicName: String,
                       casualName: String,
                       isMale: Boolean) = Action {
    Ok(views.html.frontend.celebrity_landing(
      getStartedUrl = "/David-Price/photos",
      celebrityPublicName = publicName,
      celebrityCasualName = casualName,
      landingPageImageUrl = EgraphsAssets.at("images/ortiz_masthead.jpg").url,
      celebrityGender = if (isMale) { Gender.Male } else { Gender.Female })
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
  private val marketplaceRoute = "/stars"
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

