package controllers.website.consumer

import play.mvc.Controller
import services.http.ControllerMethod
import models.{CelebrityStore, Celebrity}
import services.mvc.{ImplicitHeaderAndFooterData, CelebrityViewConversions}
import services.cache.CacheFactory
import services.Time

/**
 * The main landing page for the consume rwebsite.
 */
private[controllers] trait GetRootConsumerEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  import CelebrityViewConversions._
  import Time.IntsToSeconds.intsToSecondDurations

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def celebrityStore: CelebrityStore
  protected def cacheFactory: CacheFactory

  //
  // Controllers
  //
  /**
   * Serves the application's landing page.
   */
  def getRootConsumerEndpoint = controllerMethod() {
    val validStars = cacheFactory.applicationCache.cacheing("featured-stars", 30.seconds) {
      // Get the list of domain objects from the DB
      val featuredCelebs = celebrityStore.getFeaturedPublishedCelebrities.toIndexedSeq

      // Turn the domain objects into view (FeaturedStars), filtering out the ones
      // that were invalid due to lack of a public name or url slug.
      val featuredStars = for (celeb <- featuredCelebs; validStar <- celeb.asFeaturedStar) yield {
        validStar
      }

      // Return an IndexedSeq, which is serializable to the cache
      featuredStars
    }

    views.frontend.html.landing(featuredStars = validStars)
  }
}
