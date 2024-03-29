package monitoring.website

import scala.concurrent._
import scala.concurrent.duration._
import java.util.Date
import akka.actor._
import play.api.libs.ws._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import collections.LimitedQueue
import collections.EgraphsMetric
import monitoring.ActorUtilities
import common.CloudWatchMetricPublisher
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
    val futureResponse = WS.url(url).get()
    val futureMetricValue: Future[Int] = futureResponse.map { response => 
      if (response.status == 200) 1
      else 0
    }

    val metricValue: Int = try {
      Await.result(futureMetricValue, 5 seconds)
    } catch {
      case _: Throwable => 0
    }

    history.enqueue(metricValue)
    metricValue
  }
}