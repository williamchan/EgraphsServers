package monitoring

import common.CloudWatchMetricPublisher

trait ActorUtilities {

  def publisher: CloudWatchMetricPublisher
 
  def awsActions(namespace: String, value: Int) = {
    play.Logger.info("Send value: " + value + " to cloudwatch metric: " + namespace)
    val datum = publisher.sendData(namespace, value)
  }

}