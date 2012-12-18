package services.mvc.celebrity

import akka.actor.ActorRef
import akka.agent.Agent
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration.intToDurationInt
import akka.util.Timeout
import models.frontend.landing.CatalogStar
import services.logging.Logging
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars

/**
 * Defines the behavior of using two actors to keep a current cache of the
 * [[models.frontend.landing.CatalogStar]]s that should appear in the celebrity catalog, and to
 * query that cache.
 */
private[celebrity] trait CatalogStarsQuerying extends Logging {
  protected def catalogStarAgent: Agent[IndexedSeq[CatalogStar]]
  protected def catalogStarUpdateActor: ActorRef

  implicit val timeout = Timeout(30 seconds)

  /**
   * Grabs the current set of CatalogStars out of the cache actor, and updates
   * the cache actor if no stars were found.
   *
   * @param numUpdateAttemptsLeft number of times to attempt updating the cache in the
   *   case that no results are found before giving up and throwing an exception.
   *
   * @return the current set of stars for rendering in the celebrity catalog.
   */
  def apply(numUpdateAttemptsLeft: Int = 1): IndexedSeq[CatalogStar] = {
    val stars = catalogStarAgent.get
    // No stars had been cached. This will only happen right after an instance comes up.
    // We will instruct the update actor to provide some data immediately, block on receiving
    // a response, then re-query from the CatalogStars actor.
    if (stars.isEmpty) {
      if (shouldStopAttempting(numUpdateAttemptsLeft)) {
        log("Repeatedly failed to get landing page celebrities.")
        IndexedSeq.empty[CatalogStar]
      } else {
        getUpdatedCatalogStars(numUpdateAttemptsLeft)
      }
    } else {
      stars
    }
  }

  private def shouldStopAttempting(numUpdateAttemptsLeft: Int) = (numUpdateAttemptsLeft <= 0)

  private def getUpdatedCatalogStars(numUpdateAttemptsLeft: Int): IndexedSeq[CatalogStar] = {
    val futureOK = catalogStarUpdateActor ask UpdateCatalogStars(catalogStarAgent)

    Await.result(futureOK, 1 minutes)
    val newStars = catalogStarAgent.get
    if (newStars.isEmpty) {
      this.apply(numUpdateAttemptsLeft - 1)
    } else {
      newStars
    }
  }
}
