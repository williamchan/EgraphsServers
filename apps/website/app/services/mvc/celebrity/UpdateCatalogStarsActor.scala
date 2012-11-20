package services.mvc.celebrity

import akka.actor.{ActorRef, Actor}
import com.google.inject.Inject
import services.AppConfig
import java.util.concurrent.TimeUnit
import java.util.Random
import services.logging.Logging
import services.db.{TransactionSerializable, DBSession}
import services.cache.CacheFactory
import play.api.Play.current
import akka.actor.Props
import play.api.libs.concurrent.Akka
import akka.util.duration._
import org.joda.time.DateTimeConstants
import models.{CelebrityStore, ProductStore}

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
  import UpdateCatalogStarsActor.{UpdateCatalogStars, updatePeriodSeconds, resultsCacheKey}

  protected def receive = {
    case UpdateCatalogStars(recipientActor) => {
      // Get the stars from the cache preferentially. This reduces round-trips to the database in multi-instance
      // deployments because one instance can share the results from another.
      val cache = cacheFactory.applicationCache
      val catalogStars = cache.cacheing(resultsCacheKey, updatePeriodSeconds) {
        // Due to cache miss, this instance must update from the database. Get all the stars and
        // their sold-out info.
        db.connected(isolation = TransactionSerializable, readOnly = true) {
          log("Updating landing page celebrities")
          celebrityStore.getCatalogStars
        }
      }.toIndexedSeq

      // Send the celebs to an actor that will be in charge of serving them to
      // the landing page.
      
      log("Transmitting " + catalogStars.length + " stars to the serving actor " + recipientActor)
      recipientActor ! CatalogStarsActor.SetCatalogStars(catalogStars)

      // Send back completion. This is mostly so that the tests won't sit there waiting forever
      // for a response.
      sender ! "Done"
    }
  }
}

private[mvc] object UpdateCatalogStarsActor extends Logging {

  def init() = {
    scheduleJob()
  }

  //
  // Package members
  //
  private[celebrity] val singleton = {
    Akka.system.actorOf(Props(AppConfig.instance[UpdateCatalogStarsActor])) 
  }
  private[celebrity] val updatePeriodSeconds = 10 * DateTimeConstants.SECONDS_PER_MINUTE
  private[celebrity] val resultsCacheKey = "catalog-stars"
  private[celebrity] case class UpdateCatalogStars(recipientActor: ActorRef)

  //
  // Private members
  //
  private def scheduleJob() = {
    val random = new Random()
    val delayJitter = random.nextInt() % 10 // this should make the update schedule a little more random, and if we are unlucky that all hosts update at once, they won't the next time.
    log("Scheduling landing page celebrity update for every " + updatePeriodSeconds + "s")
    Akka.system.scheduler.schedule(
      10 seconds,
      updatePeriodSeconds seconds,
      this.singleton,
      UpdateCatalogStars(CatalogStarsActor.singleton)
    )
  }
}
