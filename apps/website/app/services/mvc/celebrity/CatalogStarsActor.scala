package services.mvc.celebrity

import akka.actor.Actor
import Actor._
import models.frontend.landing.CatalogStar
import services.logging.Logging
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.Props


/**
 * You are probably looking for [[services.mvc.celebrity.CatalogStarsQuery]] instead of this.
 *
 * This singleton actor provides a periodically-updated in-memory cache for the ViewModels that
 * appear in the catalog of all published celebrities.
 *
 * Responds to:
 *   CatalogStarsActor.GetCatalogStars
 *      Returns Some(IndexedSeq[CatalogStar]) if a cached value was found,
 *      None if no cached value was found.
 *
 *   CatalogStarsActor.SetCatalogStars
 *      Returns nothing, but sets the celeb catalog.
 */
private[celebrity] class CatalogStarsActor extends Actor with Logging {

  import CatalogStarsActor._

  private var maybeCelebs: Option[IndexedSeq[CatalogStar]] = None

  protected def receive = {
    case GetCatalogStars =>
      sender ! maybeCelebs

    case SetCatalogStars(newCelebs) =>
      log("Setting " + newCelebs.length + " new landing page celebs")
      maybeCelebs = Some(newCelebs)
  }
}

private[mvc] object CatalogStarsActor {
  val singleton = Akka.system.actorOf(Props[CatalogStarsActor])

  //
  // Package members
  //
  private[celebrity] sealed trait CatalogStarsActorMessage

  /**
   * Message that returns the cached [[models.frontend.landing.CatalogStar]]s. See
   * class documentation for how it's used.
   */
  private[celebrity] case object GetCatalogStars extends CatalogStarsActorMessage

  /**
   * Message that sets new [[models.frontend.landing.CatalogStar]]s in the cache. See
   * class documentation for how it's used. Primarily this is issued by
   * [[services.mvc.celebrity.UpdateCatalogStarsActor]]
   *
   * @param newCelebs the stars that should be set into the cache.
   */
  private[celebrity] case class SetCatalogStars(newCelebs: IndexedSeq[CatalogStar])
    extends CatalogStarsActorMessage

}
