package common

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.MetricDatum

abstract class MetricPublisher {
  
  def formatMetricDatum(namespace: String, value: Int): MetricDatum
  
  def sendData(cloudwatch: AmazonCloudWatch, datum: MetricDatum, namespace: String)

}