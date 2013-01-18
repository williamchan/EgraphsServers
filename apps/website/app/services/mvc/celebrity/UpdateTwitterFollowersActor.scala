package services.mvc.celebrity

import java.util.Random
import com.google.inject.Inject
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.agent.Agent
import akka.dispatch.Await
import akka.util.duration._
import akka.util.Timeout
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
import models.Celebrity
import akka.util.FiniteDuration
import akka.dispatch.Futures
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent._
import org.joda.time.DateTimeConstants
import twitter4j.TwitterException
import services.http.twitter.TwitterProvider

private[celebrity] class UpdateTwitterFollowersActor @Inject() (
  db: DBSession,
  cacheFactory: CacheFactory,
  celebrityStore: CelebrityStore) extends Actor with Logging {

  import UpdateTwitterFollowersActor.{ twitter, UpdateTwitterFollowers, UpdateAllTwitterFollowers, TwitterUserLookupResponse, updatePeriod, resultsCacheKey }
  implicit val timeout: Timeout = 30 seconds

  private val TWITTER_MAX_LOOKUP = 100

  protected def receive = {
    case UpdateTwitterFollowers(celebritiesWithTwitter, twitterFollowersAgent) => {
      lazy val celebrityNamesAndTwitter = celebritiesWithTwitter.map(celebrity => celebrity.publicName + " (@" + celebrity.twitterUsername.get + ")").mkString(", ")
      def reschedule = {
        Akka.system.scheduler.scheduleOnce(
          15 minutes,
          self,
          UpdateTwitterFollowers(celebritiesWithTwitter, twitterFollowersAgent))
      }
      val screenNamesToCelebrityId = celebritiesWithTwitter.map(celebrity => (celebrity.twitterUsername.get.toLowerCase, celebrity.id)).toMap.withDefaultValue(-1L)
      val twitterFollowers = try {
        import collection.JavaConversions._
        val users = twitter.lookupUsers(screenNamesToCelebrityId.keys.toArray)
        val celebrityIdAndFollowerCounts = for { user <- users } yield {
          log("Twitter user = " + user.getName + ", followers = " + user.getFollowersCount)

          val celebrityId = screenNamesToCelebrityId(user.getScreenName.toLowerCase)
          (celebrityId -> user.getFollowersCount)
        }
        celebrityIdAndFollowerCounts.toMap
      } catch {
        case e: TwitterException =>
          log("Twitter exception while updating celebrities: " + celebrityNamesAndTwitter, e)
          if (e.isCausedByNetworkIssue) {
            log("Scheduling retry.")
            reschedule
          } else {
            error("Twitter exception in handling UpdateTwitterFollowers message for celebrities: " + celebrityNamesAndTwitter, e)
          }
          Map.empty[Long, Int]
        case e =>
          error("Exception in handling UpdateTwitterFollowers message for celebrities: " + celebrityNamesAndTwitter, e)
          Map.empty[Long, Int]
      }

      sender ! TwitterUserLookupResponse(twitterFollowers)
    }

    case UpdateAllTwitterFollowers(twitterFollowersAgent) => {
      // Get the stars from the cache preferentially. This reduces round-trips to the database in multi-instance
      // deployments because one instance can share the results from another.
      val cache = cacheFactory.applicationCache
      //val twitterFollowerCounts = cache.cacheing(resultsCacheKey, updatePeriod.toSeconds.toInt) {
      //TODO FIX THIS
      val twitterFollowerCounts = {
        // Due to cache miss, this instance must update from twitter.
        val celebrities = db.connected(isolation = TransactionSerializable, readOnly = true) {
          log("Updating all celebrties' twitter followers counts")
          celebrityStore.getAll.filter(celebrity => celebrity.twitterUsername.isDefined)
        }

        // Get all twitter follower counts
        val futures = for {
          celebritiesToLookup <- celebrities.sliding(TWITTER_MAX_LOOKUP, TWITTER_MAX_LOOKUP)
        } yield {
          UpdateTwitterFollowersActor.singleton ask UpdateTwitterFollowers(celebritiesToLookup, twitterFollowersAgent)
        }

        for {
          future <- futures
          TwitterUserLookupResponse(celebrityIdsToFollowerCounts) <- future
        } {
          twitterFollowersAgent.alter(map => map ++ celebrityIdsToFollowerCounts)(timeout)
        }

        twitterFollowersAgent.await(5 minutes)
      }

      // Send back completion. This is mostly so that the tests won't sit there waiting forever
      // for a response.
      sender ! twitterFollowerCounts
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
  private[celebrity] val updatePeriod = 1 day
  private[celebrity] val resultsCacheKey = "twitter-stars"
  private[celebrity] case class UpdateAllTwitterFollowers(twitterFollowersAgent: Agent[Map[Long, Int]])
  private[celebrity] case class UpdateTwitterFollowers(celebrities: Iterable[Celebrity], twitterFollowersAgent: Agent[Map[Long, Int]])
  case class TwitterUserLookupResponse(celebrityIdsToFollowerCounts: Map[Long, Int])

  implicit val timeout: Timeout = 20 minutes

  //
  // Private members
  //
  private def scheduleJob() = {
    //run once then schedule    
    Await.result(this.singleton ask UpdateAllTwitterFollowers(TwitterFollowersAgent.singleton), 20 minutes)

    val random = new Random()
    val delayJitter = random.nextInt() % 2 hours // this should make the update schedule a little more random, and if we are unlucky that all hosts update at once, they won't the next time.
    val jitteredUpdatePeriod = updatePeriod + delayJitter
    log("Scheduling celebrity twitter data update for every " + jitteredUpdatePeriod.toSeconds + " seconds.")
    Akka.system.scheduler.schedule(
      jitteredUpdatePeriod,
      jitteredUpdatePeriod,
      this.singleton,
      UpdateAllTwitterFollowers(TwitterFollowersAgent.singleton))
  }
}
