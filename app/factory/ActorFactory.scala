package factory

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import collections.MetricSource
import akka.actor._

trait ActorFactory {

  def getInstance(metricSource: MetricSource, cloudwatch: AmazonCloudWatch): ActorRef

}