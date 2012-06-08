package controllers.website

import play.mvc.Controller
import services.http.ControllerMethod
import models.Celebrity
import services.Utils
import controllers.WebsiteControllers

private[controllers] trait GetRootEndpoint { this: Controller =>
  import views.Application._

  protected def controllerMethod: ControllerMethod

  /**
   * Serves the application's landing page.
   */
  def getRootEndpoint = controllerMethod() {
    html.index()
  }
}


object GetRootEndpoint {

  object ModelViewConversions {

    class FeaturedStarCelebrity(celebrity: Celebrity) {
      import models.frontend.landing.FeaturedStar

      def asFeaturedStar: Option[FeaturedStar] = {
        for (publicName <- celebrity.publicName; urlSlug <- celebrity.urlSlug) yield {
          FeaturedStar(
            name=publicName,
            secondaryText=Some("Free Agent"), // TODO actually populate with celebrity's team.
            imageUrl=Utils.asset("public/images/440x220_placeholder.gif"),
            storefrontUrl=WebsiteControllers.lookupGetCelebrity(urlSlug).url
          )
        }
      }
    }

    implicit def celebrityToFeaturedStarCelebrity(celebrity: Celebrity): FeaturedStarCelebrity = {
      new FeaturedStarCelebrity(celebrity)
    }
  }
}