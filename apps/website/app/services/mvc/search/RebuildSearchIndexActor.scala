package services.mvc.search

import scala.concurrent.duration._
import scala.util.Random

import com.google.inject.Inject

import akka.actor.Actor
import akka.actor.Props
import models.CelebrityStore
import play.api.Play.current
import play.api.libs.concurrent.Akka
import services.db.DBSession
import services.db.TransactionSerializable
import services.logging.Logging
import services.AppConfig

/**
 * This actor will rebuild the search index.  Realize that all hosts will try to do this.
 * We can't improve that until we can schedule a singleton job across all hosts.
 */
class RebuildSearchIndexActor @Inject() (
  db: DBSession,
  celebrityStore: CelebrityStore) extends Actor with Logging {
  import RebuildSearchIndexActor.RebuildSearchIndex

  protected def receive = {
    case RebuildSearchIndex => {
      log("Rebuilding search index starting.")
      db.connected(isolation = TransactionSerializable, readOnly = false) {
        celebrityStore.rebuildSearchIndex
      }
      log("Done rebuilding search index.")
    }
  }
}

object RebuildSearchIndexActor extends Logging {

  def init() = {
    scheduleJob()
  }

  private[search] case class RebuildSearchIndex()

  private val singleton = {
    Akka.system.actorOf(Props(AppConfig.instance[RebuildSearchIndexActor]))
  }

  private val updatePeriod = 10 minutes

  private implicit val timeout = 10 minutes

  //
  // Private members
  //
  private def scheduleJob() = {
    val random = new Random()
    val delayJitter = random.nextInt() % 1 minute // this should make the update schedule a little more random, and if we are unlucky that all hosts update at once, they won't the next time.
    val jitteredUpdatePeriod = updatePeriod + delayJitter
    log("Scheduling rebuild of search index for every " + jitteredUpdatePeriod.toSeconds + " seconds.")
    Akka.system.scheduler.schedule(
      jitteredUpdatePeriod,
      jitteredUpdatePeriod,
      this.singleton,
      RebuildSearchIndex)
  }
}
