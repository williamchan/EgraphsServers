package factory

import com.amazonaws.services.cloudwatch.AmazonCloudWatch

import akka.actor.ActorRef
import akka.actor.Props
import collections.MetricSource
import common.CloudWatchMetricPublisher
import monitoring.website.WebsiteAvailabilityActor
import play.api.Play.current
import play.api.libs.concurrent.Akka

class WebsiteActorFactory extends ActorFactory {

  def getInstance(metricSource: MetricSource,
    cloudwatch: AmazonCloudWatch): ActorRef = {

    val name = metricSource.name
    val actorName = metricSource.actorName
    val friendlyName = metricSource.friendlyName
    val publisher = new CloudWatchMetricPublisher(cloudwatch)

    return Akka.system.actorOf(Props(new WebsiteAvailabilityActor(
      name, friendlyName, publisher)), name = actorName)
  }
}