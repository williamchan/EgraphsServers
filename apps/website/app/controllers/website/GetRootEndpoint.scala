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
    views.Application.html.index()
    /*
    // Get the list of domain objects from the DB
    val featuredCelebs = celebrityStore.getFeaturedPublishedCelebrities

    // Turn the domain objects into view (FeaturedStars), filtering out the ones
    // that were invalid due to lack of a public name or url slug.
    val validStars = for (celeb <- featuredCelebs; validStar <- celeb.asFeaturedStar) yield {
      validStar
    }

    views.frontend.html.landing(validStars)
    */
  }
}


object GetRootEndpoint {

  /**
   * Converts between domain models from the back-end and view models from
   * the front-end
   */
  object ModelViewConversions {

    /**
     * Amalgam class between the Celebrity domain model and the FeaturedStar
     * view model from the landing page
     *
     * @param celebrity the celebrity to transform into a FeaturedStar
     */
    class FeaturedStarCelebrity(celebrity: Celebrity) {
      import models.frontend.landing.FeaturedStar

      /**
       * The celebrity as a FeaturedStar. If some necessary data for the FeaturedStar
       * were not available (e.g. publicName, storeFrontUrl) then it returns None.
       *
       * @return
       */
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

    /**
     * Implcitly convert Celebrity -> FeaturedStarCelebrity
     */
    implicit def celebrityToFeaturedStarCelebrity(celebrity: Celebrity): FeaturedStarCelebrity = {
      new FeaturedStarCelebrity(celebrity)
    }
  }
}