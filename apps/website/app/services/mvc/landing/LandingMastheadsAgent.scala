package services.mvc.landing

import akka.agent.Agent
import models.Masthead
import play.api.Play.current
import play.api.libs.concurrent.Akka
import models.frontend.landing.LandingMasthead


object LandingMastheadsAgent {
  val singleton = Agent(IndexedSeq.empty[LandingMasthead])(Akka.system)
}
