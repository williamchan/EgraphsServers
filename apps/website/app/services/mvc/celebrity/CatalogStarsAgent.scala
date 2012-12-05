package services.mvc.celebrity

import akka.agent.Agent
import models.frontend.landing.CatalogStar
import play.api.Play.current
import play.api.libs.concurrent.Akka

/**
 * This agent holds the catalog stars.
 */
object CatalogStarsAgent {
  val singleton = Agent(IndexedSeq.empty[CatalogStar])(Akka.system)
}
