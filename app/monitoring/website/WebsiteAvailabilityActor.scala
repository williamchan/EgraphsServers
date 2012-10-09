package monitoring.website

import akka.actor._
import play.api.libs.ws._
import play.api.libs.concurrent.Promise
import java.util.Date
import com.amazonaws.services.cloudwatch.model._
import common.MetricPublisher
import collections.LimitedQueue

case class CheckStatus()

class WebsiteAvailabilityActor(url: String, pub: MetricPublisher) extends Actor with ActorLogging {

  val history = new LimitedQueue[Int](60)

  def receive() = {

    case CheckStatus => checkStatus
    case _ => println("Cannot handle this message")
  }

  def checkStatus = {

    val webResponse = sendGetRequest
    awsActions("SiteAvailability", webResponse)
  }

  def sendGetRequest: Int = {

    val myFeed: Promise[Response] = WS.url(url).get()
    val webResponse = myFeed.await(5000).get.status.toString

    // 1 indicates site is currently available, 0 indicates unavailability
    val webResponseTransformed = if (webResponse.toInt == 200) 1 else 0

    // add to history
    history.enqueue(webResponseTransformed)
    webResponseTransformed
  }

  def awsActions(namespace: String, value: Int) = {
    val cloudwatch = utilities.Utilities.getCloudWatchClient
    val datum = pub.formatMetricDatum(namespace, value)

    // REMOVE AFTER TESTING!
    println("send to cloudwatch")
    pub.sendData(cloudwatch, datum, namespace)
  }
}