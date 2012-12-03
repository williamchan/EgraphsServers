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

  def sendGetRequest: Int = {

    val promisedResponse = WS.url(url).get()
    val promisedMetricValue: Promise[Int] = promisedResponse.map { response => 
      if (response.status == 200) 1
      else 0
    }
    
    val metricValue: Int = promisedMetricValue.await(5000).fold(
        error => 0,
        redeemedMetricValue => redeemedMetricValue
     )
     
     history.enqueue(metricValue)
     metricValue
  }
}