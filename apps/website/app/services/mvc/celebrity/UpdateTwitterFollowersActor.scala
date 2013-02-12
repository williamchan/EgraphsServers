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
import akka.util.FiniteDuration
import akka.util.Timeout
import models.Celebrity
import models.CelebrityStore
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Akka
import services.AppConfig
import services.cache.CacheFactory
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.twitter.TwitterProvider
import services.logging.Logging
import twitter4j.TwitterException

import akka.routing.SmallestMailboxRouter

// for single celebrity batch
private[celebrity] class UpdateBatchTwitterFollowersActor() extends Actor with Logging {
  import UpdateTwitterFollowersActor.{ twitter, UpdateTwitterFollowers, LateUpdateTwitterFollowers, TwitterUserLookupResponse, LateTwitterUserLookupResponse }
  protected def receive = {
    case UpdateTwitterFollowers(celebritiesWithTwitter) => {
      val twitterFollowers = lookupTwitterDataOrScheduleRetry(celebritiesWithTwitter).getOrElse(Map.empty[Long, Int])
      sender ! TwitterUserLookupResponse(twitterFollowers)
    }
    // if twitter failed before, this is how the retries are handled 
    case LateUpdateTwitterFollowers(celebritiesWithTwitter) => {
      val maybeTwitterFollowers = lookupTwitterDataOrScheduleRetry(celebritiesWithTwitter)
      maybeTwitterFollowers.map { twitterFollowers =>
        sender ! LateTwitterUserLookupResponse(twitterFollowers)
      }
    }
  }

  private def lookupTwitterDataOrScheduleRetry(celebritiesWithTwitter: Iterable[Celebrity]): Option[Map[Long, Int]] = {
    def reschedule = {
      Akka.system.scheduler.scheduleOnce(
        15 minutes,
        self,
        UpdateTwitterFollowers(celebritiesWithTwitter))
    }

    lazy val celebrityNamesAndTwitter = celebritiesWithTwitter.map(celebrity => celebrity.publicName + " (@" + celebrity.twitterUsername.get + ")").mkString(", ")
    val screenNamesToCelebrityId = celebritiesWithTwitter.map(celebrity => (celebrity.twitterUsername.get.toLowerCase, celebrity.id)).toMap.withDefaultValue(-1L)
    try {
      import collection.JavaConversions._
      val users = twitter.lookupUsers(screenNamesToCelebrityId.keys.toArray)
      val celebrityIdAndFollowerCounts = for { user <- users } yield {
        log("Twitter user = " + user.getName + ", followers = " + user.getFollowersCount)

        val celebrityId = screenNamesToCelebrityId(user.getScreenName.toLowerCase)
        (celebrityId -> user.getFollowersCount)
      }
      Some(celebrityIdAndFollowerCounts.toMap)
    } catch {
      case e: TwitterException =>
        log("Twitter exception while updating celebrities: " + celebrityNamesAndTwitter, e)
        if (e.isCausedByNetworkIssue) {
          log("Scheduling retry.")
          reschedule
        } else {
          error("Twitter exception in handling UpdateTwitterFollowers message for celebrities: " + celebrityNamesAndTwitter, e)
        }
        None
      case e: Throwable =>
        error("Exception in handling UpdateTwitterFollowers message for celebrities: " + celebrityNamesAndTwitter, e)
        None
    }
  }
}

// for all celebrities
private[celebrity] class UpdateTwitterFollowersActor @Inject() (
  db: DBSession,
  cacheFactory: CacheFactory,
  celebrityStore: CelebrityStore) extends Actor with Logging {

  val twitterFollowersAgent = TwitterFollowersAgent.singleton
  import UpdateTwitterFollowersActor.{ TwitterUserLookupResponse, updateTwitterBatchActor, UpdateTwitterFollowers, UpdateAllTwitterFollowers, LateTwitterUserLookupResponse, cachingPeriod, resultsCacheKey }
  implicit val timeout: Timeout = 30 seconds

  private val TWITTER_MAX_LOOKUP = 100


  def cache = cacheFactory.applicationCache

  protected def receive = {
    case UpdateAllTwitterFollowers => {
      val twitterFollowerCounts = cache.cacheing(resultsCacheKey, cachingPeriod.toSeconds.toInt) {
        // Due to cache miss, this instance must update from twitter.
        val celebrities = db.connected(isolation = TransactionSerializable, readOnly = true) {
          log("Updating all celebrties' twitter followers counts")
          celebrityStore.getAll.filter(celebrity => celebrity.twitterUsername.isDefined && !celebrity.doesNotHaveTwitter)
        }

        // Get all twitter follower counts
        val futures = for {
          celebritiesToLookup <- celebrities.sliding(TWITTER_MAX_LOOKUP, TWITTER_MAX_LOOKUP)
        } yield {
          updateTwitterBatchActor ask UpdateTwitterFollowers(celebritiesToLookup)
        }

        val data = {
          for {
            future <- futures
            celebrityIdToFollowerCount <- Await.result(future, 60 seconds).asInstanceOf[TwitterUserLookupResponse].celebrityIdsToFollowerCounts
          } yield {
            celebrityIdToFollowerCount
          }
        }.toMap

        data
      }

      twitterFollowersAgent.update(twitterFollowerCounts)

      // Send back completion. This is mostly so that the tests won't sit there waiting forever
      // for a response.
      sender ! twitterFollowerCounts
    }

    // if there was a failure when updating responses, this is how we will update.
    case LateTwitterUserLookupResponse(celebrityIdsToFollowerCounts) => {
      val oldFollowersCounts = cache.get[Map[Long, Int]](resultsCacheKey).getOrElse(twitterFollowersAgent())
      val newFollowersCount = oldFollowersCounts ++ celebrityIdsToFollowerCounts
      // update the agent and the cache
      twitterFollowersAgent.alter(map => map ++ celebrityIdsToFollowerCounts)(20 seconds)
      cache.set(resultsCacheKey, newFollowersCount, cachingPeriod.toSeconds.toInt)
    }
  }
}

object TwitterFollowersAgent {
  // Celebrity to FollowerCount
  val singleton = Agent(Map.empty[Long, Int])(Akka.system)
}

object UpdateTwitterFollowersActor extends Logging {

  def twitter = AppConfig.instance[TwitterProvider].get

  def init() = {
    scheduleJob()
  }

  //
  // Package members
  //
  //  private[celebrity] val singleton = {
  val singleton = {
    Akka.system.actorOf(Props(AppConfig.instance[UpdateTwitterFollowersActor]))
  }


  // Many twitter batch actors to handle the spike of requests
  val updateTwitterBatchActor = Akka.system.actorOf(Props[UpdateBatchTwitterFollowersActor].withRouter(SmallestMailboxRouter(5)), "twitterbatchrouter")

  private[celebrity] val updatePeriod = 15 minutes
  private[celebrity] val cachingPeriod = 1 day // updates more frequently to pick up changes from cache since data could have been missing from the cache earlier due to twitter failures
  private[celebrity] val resultsCacheKey = "twitter-followers-actor-data"
  private[celebrity] case class UpdateAllTwitterFollowers()
  private[celebrity] case class LateUpdateTwitterFollowers(celebrities: Iterable[Celebrity])
  private[celebrity] case class UpdateTwitterFollowers(celebrities: Iterable[Celebrity])
  private[celebrity] case class TwitterUserLookupResponse(celebrityIdsToFollowerCounts: Map[Long, Int])
  private[celebrity] case class LateTwitterUserLookupResponse(celebrityIdsToFollowerCounts: Map[Long, Int])

  implicit val timeout = 10 minutes

  //
  // Private members
  //
  private def scheduleJob() = {
    //run once then schedule    
    Await.result(this.singleton ask UpdateAllTwitterFollowers, 20 minutes)

    val random = new Random()
    val delayJitter = random.nextInt() % 1 minute // this should make the update schedule a little more random, and if we are unlucky that all hosts update at once, they won't the next time.
    val jitteredUpdatePeriod = updatePeriod + delayJitter
    log("Scheduling celebrity twitter data update for every " + jitteredUpdatePeriod.toSeconds + " seconds.")
    Akka.system.scheduler.schedule(
      jitteredUpdatePeriod,
      jitteredUpdatePeriod,
      this.singleton,
      UpdateAllTwitterFollowers)
  }
}
