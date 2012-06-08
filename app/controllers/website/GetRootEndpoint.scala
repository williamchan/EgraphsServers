package controllers.website

import play.mvc.Controller
import services.http.ControllerMethod
import services.Utils
import controllers.WebsiteControllers
import models.{CelebrityStore, Celebrity}

private[controllers] trait GetRootEndpoint { this: Controller =>
  import GetRootEndpoint.ModelViewConversions._


  protected def controllerMethod: ControllerMethod
  protected def celebrityStore: CelebrityStore

  /**
   * Serves the application's landing page.
   */
  def getRootEndpoint = controllerMethod() {
    // Get the list of domain objects from the DB
    val featuredCelebs = celebrityStore.getFeaturedPublishedCelebrities

    // Turn the domain objects into view (FeaturedStars), filtering out the ones
    // that were invalid due to lack of a public name or url slug.
    val validStars = for (celeb <- featuredCelebs; validStar <- celeb.asFeaturedStar) yield {
      validStar
    }

    views.frontend.html.landing(validStars)
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
            secondaryText=celebrity.roleDescription,
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