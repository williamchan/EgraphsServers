package services.mvc.celebrity

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.actorOf
import com.google.inject.Inject
import services.AppConfig
import java.util.concurrent.TimeUnit
import services.logging.Logging
import services.db.{TransactionSerializable, DBSession}
import services.cache.CacheFactory
import org.joda.time.DateTimeConstants
import models.ProductStore

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
 * @param productStore gets us the published celebrities
 */
private[celebrity] class UpdateCatalogStarsActor @Inject()(
  db: DBSession,
  cacheFactory: CacheFactory,
  productStore: ProductStore
) extends Actor {

  import UpdateCatalogStarsActor.{UpdateCatalogStars, resultsCacheKey}

  protected def receive = {
    case UpdateCatalogStars(recipientActor) => {
      // Get the stars from the cache preferentially. This reduces round-trips to the database in multi-instance
      // deployments because one instance can share the results from another.
      val cache = cacheFactory.applicationCache
      val catalogStars = cache.cacheing(resultsCacheKey, DateTimeConstants.SECONDS_PER_DAY) {
        // Due to cache miss, this instance must update from the database. Get all the stars and
        // their sold-out info.
        db.connected(isolation = TransactionSerializable, readOnly = true) {
          log.info("Updating landing page celebrities")

          // Get the list of catalog stars from the DB
          productStore.getCatalogStars()
        }
      }.toIndexedSeq

      // Send the celebs to an actor that will be in charge of serving them to
      // the landing page.
      log.info("Transmitting " + catalogStars.length + " stars to the serving actor " + recipientActor)
      recipientActor ! CatalogStarsActor.SetCatalogStars(catalogStars)

      // Send back completion. This is mostly so that the tests won't sit there waiting forever
      // for a response.
      self.channel ! "Done"
    }
  }
}


private[mvc] object UpdateCatalogStarsActor extends Logging {

  def init() = {
    singleton.start()

    scheduleJob()
  }

  //
  // Package members
  //
  private[celebrity] val singleton = actorOf(AppConfig.instance[UpdateCatalogStarsActor])
  private[celebrity] val updatePeriodSeconds = 30
  private[celebrity] val resultsCacheKey = "catalog-stars"
  private[celebrity] case class UpdateCatalogStars(recipientActor: ActorRef)

  //
  // Private members
  //
  private def scheduleJob() = {
    log("Scheduling landing page celebrity update for every " + updatePeriodSeconds + "s")
    akka.actor.Scheduler.schedule(
      receiver = this.singleton,
      message = UpdateCatalogStars(CatalogStarsActor.singleton),
      initialDelay = 10, // Delay first invocation for a bit to give the Redis plugin time to bootstrap
      delay = updatePeriodSeconds,
      timeUnit = TimeUnit.SECONDS
    )
  }
}
