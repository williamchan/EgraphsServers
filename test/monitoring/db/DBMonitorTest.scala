package monitoring.db

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import play.api.test.FakeApplication
import play.api.test.Helpers.running
import collections.MetricSource
import monitoring.database.DBMonitor
import factory.DBActorFactory

class DBMonitorTest extends FlatSpec with ShouldMatchers with Mockito {

  "A DBMonitor" should "return the same number of database MetricSources given at construction time" in {

    val res = running(FakeApplication()) {
      val cloudwatch = mock[AmazonCloudWatch]
      val dbsAndNames = List(MetricSource("db1", "test1AvailabilityActor", "test1"),
        MetricSource("db2", "test2AvailabilityActor", "test2"),
        MetricSource("db3", "test3AvailabilityActor", "test3"),
        MetricSource("db4", "test4AvailabilityActor", "test4"),
        MetricSource("db5", "test5AvailabilityActor", "test5"))

      val monitor = new DBMonitor(cloudwatch, 10, dbsAndNames, new DBActorFactory)
      val resultMetrics = monitor.getMetrics

      resultMetrics should have length (5)
    }
  }

}