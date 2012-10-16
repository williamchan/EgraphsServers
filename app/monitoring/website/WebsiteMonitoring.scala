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
import com.amazonaws.services.cloudwatch.model.Metric
import collections.EgraphsMetric

object WebsiteMonitoring {

  private var actors = List[ActorRef]()

  def init() = {
    val urlsAndNames = getUrlsAndNames
    scheduleWebsiteMonitoringJobs(urlsAndNames)
  }

  private def getUrlsAndNames: List[(String, String, String)] = {

    // add new URL/actor/friendly name pairs to check here
    List(("https://www.egraphs.com/", "frontPageAvailabilityActor", "frontPage"),
      ("https://www.egraphs.com/Pedro-Martinez/photos", "photoPageAvailabilityActor", "photoPage"),
      ("https://www.egraphs.com/about", "staticPageAvailabilityActor", "staticPage"))
  }

  private def scheduleWebsiteMonitoringJobs(urlsAndNames: List[(String, String, String)]) = {

    actors = for ((url, actorName, friendlyName) <- urlsAndNames) yield {
      val myCurrentActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor(url, friendlyName, new CloudWatchMetricPublisher)),
        name = actorName)
      
      // CHANGE BACK TO 1 MINUTE AFTER TESTING  
      Akka.system.scheduler.schedule(0 seconds, 10 seconds, myCurrentActor, CheckStatus)

      myCurrentActor
    }
  }

  def getMetrics: List[EgraphsMetric[Int]] = {

    var listMetricInfo = List[EgraphsMetric[Int]]()

    import akka.pattern.{ ask, pipe }
    //case class Result(url: String, friendlyName: String, history: List[Int])
    case class Result(metric: EgraphsMetric[Int])

    for (actor <- actors) yield {
      implicit val timeout = Timeout(5 seconds)

      val f: Future[Result] =
        for {
        	metric <- ask(actor, GetMetric).mapTo[EgraphsMetric[Int]]
         } yield Result(metric)

      val result = Await.result(f, timeout.duration)
      listMetricInfo = listMetricInfo ::: List(result.metric)
    }
    return listMetricInfo
  }
}