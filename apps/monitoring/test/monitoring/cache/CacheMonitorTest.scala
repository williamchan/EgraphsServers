package monitoring.cache

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import play.api.test.FakeApplication
import play.api.test.Helpers.running
import collections.MetricSource
import monitoring.database.DBMonitor
import factory.CacheActorFactory

class CacheMonitorTest extends FlatSpec with ShouldMatchers with Mockito {

  "A CacheMonitor" should "return the same number of caches given at construction time" in {

    val res = running(FakeApplication()) {
      val cloudwatch = mock[AmazonCloudWatch]
      val cacheNames = List(MetricSource("cache1", "cache1AvailabilityActor", "cache1"),
        MetricSource("cache2", "cache2AvailabilityActor", "cache2"))

      val monitor = new CacheMonitor(cloudwatch, 10, cacheNames, new CacheActorFactory)
      val resultMetrics = monitor.getMetrics

      resultMetrics should have length (2)
    }
  }

}