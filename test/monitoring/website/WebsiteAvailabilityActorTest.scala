package monitoring.website

import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.collection.immutable.List
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import common.CloudWatchMetricPublisher
import collections.EgraphsMetric
import akka.actor._
import akka.util.Timeout
import akka.dispatch.Await
import akka.dispatch.Future
import akka.util.duration._
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import play.api.test.Helpers.running
import play.api.test.FakeApplication

class WebsiteAvailabilityActorTest extends FlatSpec with ShouldMatchers with Mockito {

  "A WebsiteAvailabilityActor" should "receive an EgraphsMetric on a GetMetric request" in {
    
    val res = running(FakeApplication()) {
      val metPub = mock[CloudWatchMetricPublisher]

      val myTestActor = Akka.system.actorOf(
        Props(new WebsiteAvailabilityActor("http://isitchristmas.com/", "christmasTest", metPub)),
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