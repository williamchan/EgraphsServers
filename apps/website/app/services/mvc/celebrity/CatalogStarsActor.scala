package services.mvc.celebrity

import akka.actor.Actor
import Actor._
import models.frontend.landing.CatalogStar
import services.logging.Logging
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.Props
import akka.agent.Agent

//TODO: update this comment as best as possible
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


//TODO: rename to CatalogStarsAgent
object CatalogStarsActor {
  val singleton = Agent(IndexedSeq.empty[CatalogStar])(Akka.system)
}
