package controllers

import play.mvc.Controller
import models.frontend.storefront.{CheckoutOrderSummary, CheckoutShippingAddressFormView, CheckoutBillingInfoView, CheckoutFormView}
import org.joda.money.{CurrencyUnit, Money}

/**
 * Permutations of the Checkout: Checkout.
 */
object Egraph extends Controller with DefaultHeaderAndFooterData {

  //
  // Public members
  //
  def landscape = {
    val frame = LandscapeEgraphFrame

    views.frontend.html.egraph(
      "Herp Derpson",
      "Derp Herpson",
      frame.cssClass,
      frame.cssFrameColumnClasses,
      "/public/images/egraph_default_plaque_icon.png",
      frame.cssStoryColumnClasses,
      "Herp Derpson",
      """
        Herp Derpson, son of Herp Derpington himself, was the epitome of wisdom and class.
        Known to dip fried fish in spicy soy sauce, nary a day went by that he didn't
        fundamentally change the nature of his fans' interaction with fast food. One day
        he got a letter from Derp Herpson, and the rest, as they say, was history.
      """,
      "http://freshly-ground.com/data/audio/sm2/Adrian%20Glynn%20-%20Blue%20Belle%20Lament.mp3",
      "/public/images/sample_landscape_egraph.svg",
      "May 10, 1983",
      shareOnFacebookLink="/shareOnFacebookLink",
      shareOnTwitterLink= "/shareOnTwitterLink"  
    )
  }

  def portrait = {
    val frame = PortraitEgraphFrame

    views.frontend.html.egraph(
      "Herp Derpson",
      "Derp Herpson",
      frame.cssClass,
      frame.cssFrameColumnClasses,
      "/public/images/egraph_default_plaque_icon.png",
      frame.cssStoryColumnClasses,
      "Herp Derpson",
      """
        Herp Derpson, son of Herp Derpington himself, was the epitome of wisdom and class.
        Known to dip fried fish in spicy soy sauce, nary a day went by that he didn't
        fundamentally change the nature of his fans' interaction with fast food. One day
        he got a letter from Derp Herpson, and the rest, as they say, was history.
      """,
      "http://freshly-ground.com/data/audio/sm2/Adrian%20Glynn%20-%20Blue%20Belle%20Lament.mp3",
      "/public/images/sample_portrait_egraph.svg",
      "May 10, 1983",
      shareOnFacebookLink="/shareOnFacebookLink",
      shareOnTwitterLink= "/shareOnTwitterLink"      
    )
  }


  object PortraitEgraphFrame {
    val name: String = "Default Portrait"

    val cssClass  = "portrait"
    val cssFrameColumnClasses = "offset1 span6"
    val cssStoryColumnClasses = "span5"

    val imageWidthPixels = 377
    val imageHeightPixels = 526

    val thumbnailWidthPixels = 350
    val thumbnailHeightPixels = 525

    val pendingWidthPixels = 170
    val pendingHeightPixels = 225
  }

  object LandscapeEgraphFrame {
    val name = "Default Landscape"

    val cssClass  = "landscape"
    val cssFrameColumnClasses = "span9"
    val cssStoryColumnClasses = "span3"

    val imageWidthPixels = 595
    val imageHeightPixels = 377

    val thumbnailWidthPixels = 510
    val thumbnailHeightPixels = 410

    val pendingWidthPixels = 230
    val pendingHeightPixels = 185
  }
}

