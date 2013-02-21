package monitoring.website

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import collections.EgraphsMetric
import common.CloudWatchMetricPublisher
import common.MonitoringMessages.GetMetric
import common.MonitoringMessages.CheckStatus
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.test.FakeApplication
import play.api.test.Helpers.running

class WebsiteAvailabilityActorTest extends FlatSpec with ShouldMatchers with Mockito {

  "A WebsiteAvailabilityActor" should "receive an EgraphsMetric on a GetMetric request" in {

    val res = running(FakeApplication()) {
      val metPublisher = mock[CloudWatchMetricPublisher]

      val myTestActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor("https://www.google.com/",
          "googleTest",
          metPublisher)),
        name = "testActor")

      import akka.pattern.{ ask, pipe }
      implicit val timeout = Timeout(5 seconds)
      val futureEmptyMetric = ask(myTestActor, GetMetric).mapTo[EgraphsMetric[Int]]
      val emptyMetric = Await.result(futureEmptyMetric, 5 seconds)

      emptyMetric.name should equal("googleTest")
      emptyMetric.description should equal("https://www.google.com/")
      emptyMetric.values should have length (0)
    }
  }
}