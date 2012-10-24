package monitoring.website

import scala.collection.immutable.List
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import akka.actor.ActorRef
import akka.actor.Props
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import collections.EgraphsMetric
import common.CloudWatchMetricPublisher
import play.api.Play.current
import play.api.libs.concurrent.Akka
import monitoring.ActorUtilities
import monitoring.Monitor
import common.MonitoringMessages.CheckStatus

class WebsiteMonitor(val cloudwatch: AmazonCloudWatch,
  val interval: Int, val urlsAndNames: List[(String, String, String)])
  extends Monitor {

  //private val actors: List[ActorRef] = scheduleWebsiteMonitoringJobs

  override def scheduleMonitoringJobs: List[ActorRef] = {
    val actors = for ((url, actorName, friendlyName) <- urlsAndNames) yield {
      val myCurrentActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor(
          url,
          friendlyName,
          new CloudWatchMetricPublisher(cloudwatch))),
        name = actorName)

      Akka.system.scheduler.schedule(0 seconds, interval seconds, myCurrentActor, CheckStatus)
      myCurrentActor
    }
    actors
  }

//  def getMetrics: List[EgraphsMetric[Int]] = {
//
//    import akka.pattern.{ ask, pipe }
//    val futureMetrics = for (actor <- actors) yield {
//      implicit val timeout = Timeout(5 seconds)
//
//      for {
//        metric <- ask(actor, GetMetric).mapTo[EgraphsMetric[Int]]
//      } yield metric
//    }
//
//    for (futureMetric <- futureMetrics) yield Await.result(futureMetric, 5 seconds)
//  }
}