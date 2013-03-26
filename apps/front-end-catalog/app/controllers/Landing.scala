package controllers

import play.api.mvc._
import models.frontend.landing.{LandingMasthead, CatalogStar}
import models.frontend.marketplace.{CategoryValueViewModel, VerticalViewModel}
import helpers.DefaultImplicitTemplateParameters
import models.frontend.header.HeaderData
import egraphs.playutils.{MaleGrammar, FemaleGrammar}
import models.frontend.masthead.{SearchBoxViewModel, VideoPlayerViewModel, CallToActionViewModel, SimpleLinkViewModel}

/**
 * Permutations of the landing page
 */
object Landing extends Controller with DefaultImplicitTemplateParameters {


  def videoMasthead = Action {
    val masthead = Some(makeMasthead(VideoPlayerViewModel("Watch Video", "#")))
    Ok(views.html.frontend.landing(sampleStars.slice(0, 7), masthead, verticalViewModels = Marketplace.landingVerticalSet, marketplaceRoute  = marketplaceRoute))
  }

  def searchboxMasthead = Action {
    val masthead = Some(makeMasthead(SearchBoxViewModel("Find your stars", "#")))
    Ok(views.html.frontend.landing(sampleStars.slice(0, 7), masthead, verticalViewModels = Marketplace.landingVerticalSet, marketplaceRoute  = marketplaceRoute))
  }

  def simpleLinkMasthead = Action {
    val masthead = Some(makeMasthead(SimpleLinkViewModel("Follow this link", "http://www.google.com")))
    Ok(views.html.frontend.landing(sampleStars.slice(0, 7), masthead, verticalViewModels = Marketplace.landingVerticalSet, marketplaceRoute  = marketplaceRoute))
  }
  /**
   * Displays a permutation of the landing page with "catalog stars" counting
   * 0 <= n <= sampleStars.length
   **/
  def featuredStars(count: Int) = Action {
    Ok(views.html.frontend.landing(sampleStars.slice(0, count), None, verticalViewModels = Marketplace.landingVerticalSet, marketplaceRoute  = marketplaceRoute))
  }


  /**
   * Displays a permutation of the page with gift certificate messaging up top. 
   **/

   def giftMessaging = Action {
    implicit val headerData = HeaderData(giftCertificateLink = Some("/gift"), sessionId="1")
    Ok(views.html.frontend.landing(sampleStars, None, Marketplace.landingVerticalSet, signup = false, marketplaceRoute)(headerData, defaultFooterData, authenticityToken))
   }

  def signupOn = Action {
    val stars = sampleStars.map(star => star.copy(inventoryRemaining = 0))
    Ok(views.html.frontend.landing(sampleStars, None, Marketplace.landingVerticalSet, signup = true, marketplaceRoute))
  }

  def featuredStarsWithNoInventory() = Action {
    val stars = sampleStars.map(star => star.copy(inventoryRemaining = 0))
    Ok(views.html.frontend.landing(stars, None, Marketplace.landingVerticalSet, signup = false, marketplaceRoute))
  }

  def singleCelebrity(publicName: String,
                       casualName: String,
                       isMale: Boolean) = Action {

    val masthead = LandingMasthead(
      headline = "Get an egraph from " + publicName,
      landingPageImageUrl = sampleMastheadUrl,
      callToActionViewModel = SimpleLinkViewModel(text = "Get Started", target = "#")
    )

    Ok(views.html.frontend.celebrity_landing(
      getStartedUrl = "/David-Price/photos",
      celebrityPublicName = publicName,
      celebrityCasualName = casualName,
      landingPageImageUrl = EgraphsAssets.at("images/ortiz_masthead.jpg").url,
      celebrityGrammar = if (isMale) { MaleGrammar } else { FemaleGrammar },
      masthead
    ))
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

  private def makeMasthead(callToActionViewModel: CallToActionViewModel) = {
    LandingMasthead(
      headline = "Direct the user's attention",
      subtitle = Some("Click here!"),
      landingPageImageUrl = sampleMastheadUrl,
      callToActionViewModel = callToActionViewModel
    )
  }

  private val sampleImageUrl = "http://placehold.it/440x157"
  private val sampleMarketplaceImageUrl = EgraphsAssets.at("images/660x350.gif").url
  private val sampleMastheadUrl = EgraphsAssets.at("images/ortiz_masthead.jpg").url
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

