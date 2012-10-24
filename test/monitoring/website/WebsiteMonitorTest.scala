package monitoring.website

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import play.api.test.FakeApplication
import play.api.test.Helpers.running

class WebsiteMonitorTest extends FlatSpec with ShouldMatchers with Mockito {

  "A WebsiteMonitor" should "return the same number of URLs given at construction time" in {

    val res = running(FakeApplication()) {
      val cloudwatch = mock[AmazonCloudWatch]
      val urlsAndNames = List(("https://www.test1.com/", "test1AvailabilityActor", "test1"),
        ("https://www.test2.com/", "test2AvailabilityActor", "test2"),
        ("https://www.test3.com/", "test3AvailabilityActor", "test3"),
        ("https://www.test4.com/", "test4AvailabilityActor", "test4"),
        ("https://www.test5.com/", "test5AvailabilityActor", "test5"))

      val monitor = new WebsiteMonitor(cloudwatch, 10, urlsAndNames)
      val resultMetrics = monitor.getMetrics

      resultMetrics should have length (5)
    }
  }
}