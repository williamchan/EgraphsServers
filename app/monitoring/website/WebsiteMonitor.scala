package monitoring.website

import scala.collection.immutable.List

import com.amazonaws.services.cloudwatch.AmazonCloudWatch

import akka.actor.ActorRef
import collections.MetricSource
import factory.WebsiteActorFactory
import monitoring.Monitor

class WebsiteMonitor(val cloudwatch: AmazonCloudWatch,
  val interval: Int, val actorInfos: List[MetricSource], 
  val actorFactory: WebsiteActorFactory) extends Monitor {
  
  protected val actors: List[ActorRef] = scheduleMonitoringJobs(actorFactory, 
      actorInfos, cloudwatch, interval)
}