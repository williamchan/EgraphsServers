package services.mvc.landing

import com.google.inject.Inject
import services.db.{TransactionSerializable, DBSession}
import services.cache.CacheFactory
import models.MastheadStore
import akka.actor.{Props, Actor}
import services.logging.Logging
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import services.AppConfig
import services.mvc.landing.UpdateLandingMastheadsActor.UpdateLandingMastheads
import models.frontend.landing.LandingMasthead
import akka.agent.Agent
import play.api.Play.current
import akka.pattern.ask
import akka.util.Timeout
import util.Random
import concurrent._
import concurrent.duration._


private[landing] class UpdateLandingMastheadsActor @Inject()(
  db: DBSession,
  cacheFactory: CacheFactory,
  mastheadStore: MastheadStore
) extends Actor with Logging {

  import UpdateLandingMastheadsActor.{resultsCacheKey, updatePeriod}

  def receive = {
    case UpdateLandingMastheads(landingMastheadsAgent) => {

      val cache = cacheFactory.applicationCache
      val tempLandingMastheads = cache.cacheing(resultsCacheKey, updatePeriod.toSeconds.toInt) {
        db.connected(isolation = TransactionSerializable, readOnly = true) {
          log("Updating mastheads")
          mastheadStore.getLandingMastheads
        }
      }

      val landingMastheads = if (tempLandingMastheads == null || tempLandingMastheads.isEmpty) {
        IndexedSeq()
      } else {
        tempLandingMastheads.toIndexedSeq
      }

      log("Transmitting " + landingMastheads.length + " mastheads to the agent.")
      landingMastheadsAgent send landingMastheads
      landingMastheadsAgent.await(10 seconds)

      sender ! landingMastheadsAgent.get
    }
  }
}

object UpdateLandingMastheadsActor extends Logging {
  def init() = {
    scheduleJob()
  }

  private[landing] val singleton = {
    Akka.system.actorOf(Props(AppConfig.instance[UpdateLandingMastheadsActor]))
  }
  private[landing] val updatePeriod = 2 minutes
  private[landing] val resultsCacheKey = "landing-masthead"
  private[landing] case class UpdateLandingMastheads(landingMastheadsAgent: Agent[IndexedSeq[LandingMasthead]])

  implicit val timeout: Timeout = 2 minutes

  private def scheduleJob() = {
    Await.result(this.singleton ask UpdateLandingMastheads(LandingMastheadsAgent.singleton), 2 minutes)

    val random = new Random()
    val delayJitter = random.nextInt() % 10 seconds
    val jitteredUpdatePeriod = updatePeriod + delayJitter
    log("Schedule landing masthead update for every " + jitteredUpdatePeriod.toSeconds + " seconds.")
    Akka.system.scheduler.schedule(
      jitteredUpdatePeriod,
      jitteredUpdatePeriod,
      this.singleton,
      UpdateLandingMastheads(LandingMastheadsAgent.singleton)
    )
  }
}

