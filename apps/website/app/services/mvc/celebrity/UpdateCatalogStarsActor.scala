package services.mvc.celebrity

import scala.concurrent._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Random
import com.google.inject.Inject
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.agent.Agent
import akka.pattern.ask
import models.CelebrityStore
import models.frontend.landing.CatalogStar
import play.api.Play.current
import play.api.libs.concurrent.Akka
import services.cache.CacheFactory
import services.db.DBSession
import services.db.TransactionSerializable
import services.logging.Logging
import services.AppConfig

/**
 * You are probably looking for [[services.mvc.celebrity.CatalogStarsQuery]] instead of this.
 *
 * A scheduled actor that keeps the [[models.frontend.landing.CatalogStar]] singleton instance
 * up-to-date with the current set of [[models.frontend.landing.CatalogStar]]s.
 *
 * @param db database connection for accessing the current CatalogStars
 *   should the cache lack them
 * @param cacheFactory cache connection that retrieves the CatalogStars as the first
 *   attempt before going to the DB.
 * @param celebrityStore gets us the published celebrities
 * @param viewConverting functionality to turn [[models.Celebrity]] instances into CatalogStars.
 */
private[celebrity] class UpdateCatalogStarsActor @Inject()(
  db: DBSession,
  cacheFactory: CacheFactory,
  celebrityStore: CelebrityStore,
  viewConverting: CelebrityViewConverting
) extends Actor with Logging {

  import viewConverting._
  import UpdateCatalogStarsActor.{UpdateCatalogStars, updatePeriod, resultsCacheKey}

  protected def receive = {
    case UpdateCatalogStars(catalogStarsAgent) => {
      // Get the stars from the cache preferentially. This reduces round-trips to the database in multi-instance
      // deployments because one instance can share the results from another.
      val cache = cacheFactory.applicationCache
      val tempCatalogStars = cache.cacheing(resultsCacheKey, updatePeriod.toSeconds.toInt) {
        // Due to cache miss, this instance must update from the database. Get all the stars and
        // their sold-out info.
        db.connected(isolation = TransactionSerializable, readOnly = true) {
          log("Updating landing page celebrities")
          celebrityStore.getCatalogStars
        }
      }

      val catalogStars = if (tempCatalogStars == null || tempCatalogStars.isEmpty) {
        IndexedSeq()
      } else {
        tempCatalogStars.toIndexedSeq
      }
      
      // Send the celebs to an actor that will be in charge of serving them to
      // the landing page.
      
      log("Transmitting " + catalogStars.length + " stars to the agent.")
      catalogStarsAgent send catalogStars
      catalogStarsAgent.await(10 seconds)

      // Send back completion. This is mostly so that the tests won't sit there waiting forever
      // for a response.
      sender ! catalogStarsAgent.get
    }
  }
}

object UpdateCatalogStarsActor extends Logging {

  def init() = {
    scheduleJob()
  }

  //
  // Package members
  //
  private[celebrity] val singleton = {
    Akka.system.actorOf(Props(AppConfig.instance[UpdateCatalogStarsActor]))
  }
  private[celebrity] val updatePeriod = 2 minutes
  private[celebrity] val resultsCacheKey = "catalog-stars"
  private[celebrity] case class UpdateCatalogStars(catalogStarsAgent: Agent[IndexedSeq[CatalogStar]])

  implicit val timeout = 2 minutes

  //
  // Private members
  //
  private def scheduleJob() = {
    //run once then schedule    
    Await.result(this.singleton ask UpdateCatalogStars(CatalogStarsAgent.singleton), 2 minutes)

    val random = new Random()
    val delayJitter = random.nextInt() % 10 seconds // this should make the update schedule a little more random, and if we are unlucky that all hosts update at once, they won't the next time.
    val jitteredUpdatePeriod = updatePeriod + delayJitter
    log("Scheduling landing page celebrity update for every " + jitteredUpdatePeriod.toSeconds + " seconds.")
    Akka.system.scheduler.schedule(
      jitteredUpdatePeriod,
      jitteredUpdatePeriod,
      this.singleton,
      UpdateCatalogStars(CatalogStarsAgent.singleton)
    )
  }
}
