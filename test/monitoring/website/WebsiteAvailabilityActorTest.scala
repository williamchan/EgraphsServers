package monitoring.website

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito

import akka.actor.Props
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import collections.EgraphsMetric
import common.CloudWatchMetricPublisher
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.test.FakeApplication
import play.api.test.Helpers.running
import monitoring.AvailabilityActor

class WebsiteAvailabilityActorTest extends FlatSpec with ShouldMatchers with Mockito {

  "A WebsiteAvailabilityActor" should "receive an EgraphsMetric on a GetMetric request" in {
    
    val res = running(FakeApplication()) {
      val metPub = mock[CloudWatchMetricPublisher]
      val availabilityActor = new AvailabilityActor(metPub)

      val myTestActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor("http://isitchristmas.com/", "christmasTest", availabilityActor)),
        name = "testActor")

      import akka.pattern.{ ask, pipe }
      implicit val timeout = Timeout(5 seconds)
      val futureMetric = ask(myTestActor, GetMetric).mapTo[EgraphsMetric[Int]]
      val metric = Await.result(futureMetric, 5 seconds)

      metric.name should equal("christmasTest")
      metric.description should equal("http://isitchristmas.com/")
      metric.values should have length (0)
    }
  }
}