package monitoring.website

import akka.actor._
import play.api.libs.ws._
import play.api.libs.concurrent.Promise
import java.util.Date
import common.CloudWatchMetricPublisher
import collections.LimitedQueue
import collections.EgraphsMetric

case class CheckStatus()
case class GetMetric()

class WebsiteAvailabilityActor(url: String, friendlyName: String, pub: CloudWatchMetricPublisher)
  extends Actor with ActorLogging {

  private val history = new LimitedQueue[Int](60)

  def receive() = {

    case CheckStatus => checkStatus
    case GetMetric => sender ! EgraphsMetric(friendlyName, url, history.toIndexedSeq)
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
    play.Logger.info("Send value: " + value + " to cloudwatch metric: " + namespace)
    val datum = pub.sendData(namespace, value)
  }
}