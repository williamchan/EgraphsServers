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
import factory.DBActorFactory

class DBMonitor(val cloudwatch: AmazonCloudWatch,
  val interval: Int, val actorInfos: List[MetricSource], val actorFactory: DBActorFactory)
  extends Monitor {

  protected val actors: List[ActorRef] = scheduleMonitoringJobs(actorFactory, 
      actorInfos, cloudwatch, interval)
}