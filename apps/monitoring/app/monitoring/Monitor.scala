package monitoring

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import com.amazonaws.services.cloudwatch.AmazonCloudWatch

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import collections.EgraphsMetric
import collections.MetricSource
import common.MonitoringMessages.CheckStatus
import common.MonitoringMessages.GetMetric
import factory.ActorFactory
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait Monitor {

  protected val actors: List[ActorRef]

  /**
   * Note: The Akka scheduler was running into deadlock issues due to multiple ClassLoaders
   * trying to access the same resource. By giving the scheduler a 2-second delay, we avoid
   * such problems, in Akka.system.schedule.schedule(2 seconds ...)
   */
  def scheduleMonitoringJobs(actorFactory: ActorFactory, actorInfos: List[MetricSource],
    cloudwatch: AmazonCloudWatch, interval: Int): List[ActorRef] = {

    val actors = for (actorInfo <- actorInfos) yield {
      val myCurrentActor = actorFactory.getInstance(actorInfo, cloudwatch)
      Akka.system.scheduler.schedule(2 seconds, interval seconds, myCurrentActor, CheckStatus)
      myCurrentActor
    }
    actors
  }

  /**
   * Gets recent metrics from a particular actor.
   */
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