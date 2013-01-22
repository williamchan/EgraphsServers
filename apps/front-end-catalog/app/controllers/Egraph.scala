package controllers

import play.api._
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
  def landscape(isPromotional: Boolean = false) = Action {
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

  def portrait(isPromotional: Boolean = false) = Action  {
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

