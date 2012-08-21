package services.mvc.celebrity

import akka.actor.ActorRef
import models.frontend.landing.CatalogStar
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars
import services.mvc.celebrity.CatalogStarsActor.GetCatalogStars

/**
 * Defines the behavior of using two actors to keep a current cache of the
 * [[models.frontend.landing.CatalogStar]]s that should appear in the celebrity catalog, and to
 * query that cache.
 */
private[celebrity] trait CatalogStarsQuerying {
  protected def catalogStarActor: ActorRef
  protected def catalogStarUpdateActor: ActorRef

  /**
   * Grabs the current set of CatalogStars out of the cache actor, and updates
   * the cache actor if no stars were found.
   *
   * @param numUpdateAttempts number of times to attempt updating the cache in the
   *   case that no results are found before giving up and throwing an exception.
   *
   * @return the current set of stars for rendering in the celebrity catalog.
   */
  def apply(numUpdateAttempts: Int = 1): IndexedSeq[CatalogStar] = {
    val maybeStars = (catalogStarActor !! GetCatalogStars).getOrElse {
      error("Received no response from CatalogStarsActor. This is 100% programmer error")

      None
    }

    maybeStars match {
      // Some stars had already been cached. This should almost always be the case.
      case Some(stars: IndexedSeq[CatalogStar]) =>
        stars

      // No stars had been cached. This will only happen right after an instance comes up.
      // We will instruct the update actor to provide some data immediately, block on receiving
      // a response, then re-query from the CatalogStars actor.
      case None =>
        if (numUpdateAttempts > 0) {
          catalogStarUpdateActor !! UpdateCatalogStars(catalogStarActor)

          this.apply(numUpdateAttempts = numUpdateAttempts - 1)
        } else {
          throw new Exception("Repeatedly failed to get landing page celebrities.")
        }

      // wtf why would it give us something besides an IndexedSeq[CatalogStar]?
      case Some(otherType) =>
        throw new Exception(
          "Unexpected response from CatalogStarsActor: " + otherType
        )
    }
  }
}
