package monitoring.database

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
import collections.MetricSource

class DBMonitor(val cloudwatch: AmazonCloudWatch,
  val interval: Int, val dbsAndNames: List[MetricSource])
  extends Monitor {

  override def scheduleMonitoringJobs: List[ActorRef] = {
    val actors = for (MetricSource(database, actorName, friendlyName) <- dbsAndNames) yield {
      val myCurrentActor = Akka.system.actorOf(
        Props(new DBAvailabilityActor(
          database,
          friendlyName,
          new CloudWatchMetricPublisher(cloudwatch))),
        name = actorName)

      Akka.system.scheduler.schedule(0 seconds, interval seconds, myCurrentActor, CheckStatus)
      myCurrentActor
    }
    actors
  }
}