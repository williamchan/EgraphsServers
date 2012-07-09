package controllers.website.consumer

import play.mvc.Controller
import services.http.ControllerMethod
import models.{CelebrityStore, Celebrity}
import services.mvc.{ImplicitHeaderAndFooterData, CelebrityViewConversions}

/**
 * The main landing page for the consume rwebsite.
 */
private[controllers] trait GetRootConsumerEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  import CelebrityViewConversions._

  protected def controllerMethod: ControllerMethod
  protected def celebrityStore: CelebrityStore

  /**
   * Serves the application's landing page.
   */
  def getRootConsumerEndpoint = controllerMethod() {
    // Get the list of domain objects from the DB
    val featuredCelebs = celebrityStore.getFeaturedPublishedCelebrities

    // Turn the domain objects into view (FeaturedStars), filtering out the ones
    // that were invalid due to lack of a public name or url slug.
    val validStars = for (celeb <- featuredCelebs; validStar <- celeb.asFeaturedStar) yield {
      validStar
    }

    views.frontend.html.landing(featuredStars = validStars)
  }
}
