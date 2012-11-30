package common

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.MetricDatum
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import com.amazonaws.auth._

import java.util.Date

class CloudWatchMetricPublisher(val cloudwatch: AmazonCloudWatch) {

  private def formatMetricDatum(namespace: String, value: Int): MetricDatum = {

    new MetricDatum()
      .withDimensions(new Dimension()
        .withName("Availability")
        .withValue("m1.small"))
      .withMetricName(namespace)
      .withTimestamp(new Date())
      .withUnit("Count")
      .withValue(value)
  }

  def sendData(namespace: String, value: Int) = {
	val datum = formatMetricDatum(namespace, value)
    val request = new PutMetricDataRequest().withNamespace(datum.getMetricName).withMetricData(datum)
    cloudwatch.putMetricData(request)
  }
}