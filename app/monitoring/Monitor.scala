package monitoring

import scala.collection.immutable.List
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import akka.actor.ActorRef
import akka.actor.Props
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import collections.EgraphsMetric
import common.CloudWatchMetricPublisher
import play.api.Play.current
import play.api.libs.concurrent.Akka
import common.MonitoringMessages.CheckStatus
import common.MonitoringMessages.GetMetric

trait Monitor {

  protected val actors: List[ActorRef] = scheduleMonitoringJobs

  protected def scheduleMonitoringJobs: List[ActorRef]

  def getMetrics: List[EgraphsMetric[Int]] = {
    import akka.pattern.{ ask, pipe }
    val futureMetrics = for (actor <- actors) yield {
      implicit val timeout = Timeout(5 seconds)

      for {
        metric <- ask(actor, GetMetric).mapTo[EgraphsMetric[Int]]
      } yield metric
    }
    for (futureMetric <- futureMetrics) yield Await.result(futureMetric, 5 seconds)
  }
}