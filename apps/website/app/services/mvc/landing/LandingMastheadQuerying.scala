package services.mvc.landing

import services.logging.Logging
import models.frontend.landing.LandingMasthead
import akka.actor.ActorRef
import akka.agent.Agent
import concurrent._
import scala.concurrent.duration._
import akka.pattern.ask
import services.mvc.landing.UpdateLandingMastheadsActor.UpdateLandingMastheads
import akka.util

private[landing] trait LandingMastheadQuerying extends Logging {
  protected def landingMastheadAgent: Agent[IndexedSeq[LandingMasthead]]
  protected def landingMastheadUpdateActor: ActorRef

  implicit val timeout = util.Timeout(30 seconds)

  /**
   * Grabs the current set of CatalogStars out of the cache actor, and updates
   * the cache actor if no stars were found.
   *
   * @param numUpdateAttemptsLeft number of times to attempt updating the cache in the
   *   case that no results are found before giving up and throwing an exception.
   *
   * @return the current set of stars for rendering in the celebrity catalog.
   */
  def apply(numUpdateAttemptsLeft: Int = 1): IndexedSeq[LandingMasthead] = {
    val mastheads = landingMastheadAgent.get
    if (mastheads.isEmpty) {
      if (shouldStopAttempting(numUpdateAttemptsLeft)) {
        log("Repeatedly failed to get mastheads")
        IndexedSeq.empty[LandingMasthead]
      } else {
        getUpdatedLandingMastheads(numUpdateAttemptsLeft)
      }
    } else {
      mastheads
    }
  }

  private def shouldStopAttempting(numUpdateAttemptsLeft: Int) = (numUpdateAttemptsLeft <= 0)

  private def getUpdatedLandingMastheads(numUpdateAttemptsLeft: Int): IndexedSeq[LandingMasthead] = {
    val futureOK = landingMastheadUpdateActor ask UpdateLandingMastheads(landingMastheadAgent)

    Await.result(futureOK, 1 minutes)
    val landingMastheads = landingMastheadAgent.get
    if (landingMastheads.isEmpty) {
      this.apply(numUpdateAttemptsLeft - 1)
    } else {
      landingMastheads
    }
  }
}
