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
import collections.MetricSource
import factory.ActorFactory

trait Monitor {

  protected val actors: List[ActorRef]

  def scheduleMonitoringJobs(actorFactory: ActorFactory, actorInfos: List[MetricSource],
    cloudwatch: AmazonCloudWatch, interval: Int): List[ActorRef] = {

    val actors = for (actorInfo <- actorInfos) yield {
      val myCurrentActor = actorFactory.getInstance(actorInfo, cloudwatch)
      Akka.system.scheduler.schedule(2 seconds, interval seconds, myCurrentActor, CheckStatus)
      myCurrentActor
    }
    actors
  }

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