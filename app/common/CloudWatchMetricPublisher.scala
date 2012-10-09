package common

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.MetricDatum
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import com.amazonaws.auth._

import java.util.Date

class CloudWatchMetricPublisher extends MetricPublisher {

  override def formatMetricDatum(namespace: String, value: Int): MetricDatum = {

    new MetricDatum().
      withDimensions(new Dimension()
        .withName("Availability")
        .withValue("m1.small"))
      .withMetricName(namespace)
      .withTimestamp(new Date())
      .withUnit("Count")
      .withValue(value)
  }

  def sendData(cloudwatch: AmazonCloudWatch, datum: MetricDatum, namespace: String) = {

    val request = new PutMetricDataRequest().withNamespace(namespace).withMetricData(datum)
    cloudwatch.putMetricData(request)

  }
}