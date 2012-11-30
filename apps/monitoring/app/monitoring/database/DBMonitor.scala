package monitoring.database

import scala.collection.immutable.List

import com.amazonaws.services.cloudwatch.AmazonCloudWatch

import akka.actor.ActorRef
import collections.MetricSource
import factory.DBActorFactory
import monitoring.Monitor

class DBMonitor(val cloudwatch: AmazonCloudWatch,
  val interval: Int, val actorInfos: List[MetricSource], val actorFactory: DBActorFactory)
  extends Monitor {

  protected val actors: List[ActorRef] = scheduleMonitoringJobs(actorFactory,
    actorInfos, cloudwatch, interval)
}