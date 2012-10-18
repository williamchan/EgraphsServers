package monitoring.website

import play.api.Play.current
import play.api.libs.concurrent.Akka
import scala.collection.immutable.List
import akka.actor._
import akka.dispatch.Await
import akka.dispatch.Future
import akka.util.Timeout
import akka.util.duration._
import collections.EgraphsMetric
import common.CloudWatchMetricPublisher
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.auth.BasicAWSCredentials

object WebsiteMonitoring {

  private val actors: List[ActorRef] = scheduleWebsiteMonitoringJobs(urlsAndNames)

  private def urlsAndNames: List[(String, String, String)] = {
    // add new URL/actor/friendly name pairs to check here
    List(("https://www.egraphs.com/", "frontPageAvailabilityActor", "frontPage"),
      ("https://www.egraphs.com/Pedro-Martinez/photos", "photoPageAvailabilityActor", "photoPage"),
      ("https://www.egraphs.com/about", "staticPageAvailabilityActor", "staticPage"))
  }

  private def scheduleWebsiteMonitoringJobs(urlsAndNames: List[(String, String, String)]): List[ActorRef] = {
    val cloudwatch = getCloudWatchClient
    val actors = for ((url, actorName, friendlyName) <- urlsAndNames) yield {
      val myCurrentActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor(url, friendlyName, new CloudWatchMetricPublisher(cloudwatch))),
        name = actorName)

      // CHANGE BACK TO 1 MINUTE AFTER TESTING  
      Akka.system.scheduler.schedule(0 seconds, 10 seconds, myCurrentActor, CheckStatus)

      myCurrentActor
    }
    actors
  }

  def getMetrics: List[EgraphsMetric[Int]] = {

    import akka.pattern.{ ask, pipe }
    val futureMetrics = for (actor <- actors) yield {
      implicit val timeout = Timeout(5 seconds)

      for {
        metric <- ask(actor, GetMetric).mapTo[EgraphsMetric[Int]]
      } yield metric
    }

    for (futureMetric <- futureMetrics) yield Await.result(futureMetric, 5 seconds)
  }

  private def getCloudWatchClient: AmazonCloudWatch = {

    val credentials = new BasicAWSCredentials("AKIAIERATN4HQDXFZMJA",
      "C0X33XoddmG7F9T3xALK7NSufU0V7Fv6wxoz6vfs")

    val cloudwatch: AmazonCloudWatch = new AmazonCloudWatchClient(credentials)
    cloudwatch.setEndpoint("monitoring.us-east-1.amazonaws.com")

    cloudwatch

  }
}