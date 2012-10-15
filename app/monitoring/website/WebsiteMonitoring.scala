package monitoring.website

import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.Future
import akka.dispatch.Await

import scala.collection.immutable.HashMap
import scala.collection.immutable.List

import common.CloudWatchMetricPublisher
import java.util.ArrayList
import play.libs.Akka

object WebsiteMonitoring {

  private var urlsAndNames = List[(String, String)]()
  private var actors = List[ActorRef]()

  def init() = {
    getUrlsAndNames
    scheduleWebsiteMonitoringJobs
  }

  private def getUrlsAndNames = {

    // add new URL/actor name pairs to check here
    urlsAndNames = List(("https://www.egraphs.com/", "frontPageAvailabilityActor"),
      ("https://www.egraphs.com/Pedro-Martinez/photos", "photoPageAvailabilityActor"),
      ("https://www.egraphs.com/about", "staticPageeAvailabilityActor"))
  }

  private def scheduleWebsiteMonitoringJobs = {

    actors = for ((url, actorName) <- urlsAndNames) yield {
      val myCurrentActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor(url, new CloudWatchMetricPublisher)),
        name = actorName)
      Akka.system.scheduler.schedule(0 seconds, 1 minute, myCurrentActor, CheckStatus)

      myCurrentActor
    }
  }

  def getActorInfo: HashMap[String, List[Int]] = {

    var map = HashMap[String, List[Int]]()

    import akka.pattern.{ ask, pipe }
    case class Result(url: String, history: List[Int])

    for ((actor) <- actors) yield {
      implicit val timeout = Timeout(5 seconds)

      val f: Future[Result] =
        for {
          url <- ask(actor, GetUrl).mapTo[String]
          history <- ask(actor, GetHistory).mapTo[List[Int]]
        } yield Result(url, history)

      val result = Await.result(f, timeout.duration)
      map += (result.url -> result.history)
    }
    return map
  }
}