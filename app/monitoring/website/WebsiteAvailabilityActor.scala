package monitoring.website

import akka.actor._
import play.api.libs.ws._
import play.api.libs.concurrent.Promise
import java.util.Date
import common.CloudWatchMetricPublisher
import collections.LimitedQueue
import collections.EgraphsMetric
import monitoring.ActorUtilities
import common.MonitoringMessages.CheckStatus
import common.MonitoringMessages.GetMetric

class WebsiteAvailabilityActor(url: String, friendlyName: String,
  val publisher: CloudWatchMetricPublisher)
  extends Actor with ActorUtilities with ActorLogging {

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

  /** TODO: Indicate failure to Cloudwatch on timeout */
  def sendGetRequest: Int = {

    val promisedResponse: Promise[Response] = WS.url(url).get()
    val webResponse = promisedResponse.await(5000).get.status

    /** 1 indicates site is currently available, 0 indicates unavailability */
    val webResponseTransformed = if (webResponse == 200) 1 else 0

    /** add to history */
    history.enqueue(webResponseTransformed)
    webResponseTransformed

    //    val promisedResponse: Promise[Response] = WS.url(url).get()
    //    val promisedMetricValue: Promise[Int] = promisedResponse.map {
    //      case 200 => 1
    //      case _ => 0
    //    }
    //    
    //    val metricValue = promisedResponse.await(5000).fold(
    //        error => 0,
    //        redeemedMetricValue => redeemedMetricValue
    //     )

    //    try {
    //      val webResponse = promisedReponse.await(5000).get.status
    //    } catch {
    //      /** consider a 5-second timeout to mean site is unavailable */
    //      case ex: java.util.concurrent.TimeoutException => return 0
    //    }
  }
}