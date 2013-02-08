package controllers

import play.api.mvc._
import models.frontend.egraph.{LandscapeEgraphFrameViewModel, PortraitEgraphFrameViewModel}
import helpers.DefaultImplicitTemplateParameters

/**
 * Permutations of the Checkout: Checkout.
 */
object Egraph extends Controller with DefaultImplicitTemplateParameters {

  //
  // Public members
  //
  def egraph() = Action {
    Ok(views.html.frontend.egraph(
      mp4Url = EgraphsAssets.at("videos/sample_egraph_video.mp4").url,
      videoPosterUrl = EgraphsAssets.at("images/sample_egraph_poster.jpg").url,
      celebrityName = "Sergio Romo",
      celebrityTagline = "San Francisco Giants",
      recipientName = "Jordan",
      privacySetting = "Public",
      messageToCelebrity = Some("Hey Sergio! You throw a mean pitch. What awesome beard trimming tips do you have?"),
      productIconUrl = EgraphsAssets.at("images/sample_product_icon.png").url,
      signedOnDate = "January 1, 2013",
      thisPageLink = "https://www.egraphs.com/stars",
      classicPageLink = "#",
      shareOnPinterestLink = "http://pinterest.com/pin/create/button/?url=https%3A%2F%2Fwww.egraphs.com&media=https%3A%2F%2Fd3kp0rxeqzwisk.cloudfront.net%2Fassets%2Fimages%2Fcrowd-fp-2054508835.jpg&description=Share%20a%20moment%20with%20your%20star",
      tweetText = "An egraph for Jordan from Sergio Romo",
      isPromotional = false
    ))
  }

  def classicLandscape(isPromotional: Boolean = false) = Action {
    val frame = LandscapeEgraphFrameViewModel

    Ok(views.html.frontend.egraph_classic(
      "Herp Derpson",
      "Derp Herpson",
      frame.cssClass,
      frame.cssFrameColumnClasses,
      EgraphsAssets.at("images/egraph_default_plaque_icon.png").url,
      frame.cssStoryColumnClasses,
      "Herp Derpson",
      """
        Herp Derpson, son of Herp Derpington himself, was the epitome of wisdom and class.
        Known to dip fried fish in spicy soy sauce, nary a day went by that he didn't
        fundamentally change the nature of his fans' interaction with fast food. One day
        he got a letter from Derp Herpson, and the rest, as they say, was history.
      """,
      "http://freshly-ground.com/data/audio/sm2/Adrian%20Glynn%20-%20Blue%20Belle%20Lament.mp3",
      EgraphsAssets.at("images/sample_landscape_egraph.svg").url,
      "May 10, 1983",
      shareOnFacebookLink="/shareOnFacebookLink",
      shareOnTwitterLink= "This is my test egraph",
      isPromotional = isPromotional
    ))
  }

  def classicPortrait(isPromotional: Boolean = false) = Action  {
    val frame = PortraitEgraphFrameViewModel

    Ok(views.html.frontend.egraph_classic(
      "Herp Derpson",
      "Derp Herpson",
      frame.cssClass,
      frame.cssFrameColumnClasses,
      EgraphsAssets.at("images/egraph_default_plaque_icon.png").url,
      frame.cssStoryColumnClasses,
      "Herp Derpson",
      """
        Herp Derpson, son of Herp Derpington himself, was the epitome of wisdom and class.
        Known to dip fried fish in spicy soy sauce, nary a day went by that he didn't
        fundamentally change the nature of his fans' interaction with fast food. One day
        he got a letter from Derp Herpson, and the rest, as they say, was history.
      """,
      "http://freshly-ground.com/data/audio/sm2/Adrian%20Glynn%20-%20Blue%20Belle%20Lament.mp3",
      EgraphsAssets.at("images/sample_portrait_egraph.svg").url,
      "May 10, 1983",
      shareOnFacebookLink="/shareOnFacebookLink",
      shareOnTwitterLink= "This is my test egraph!",
      isPromotional = isPromotional
    ))
  }
}

