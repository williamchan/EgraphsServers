package common

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest

class CloudWatchMetricPublisherTest extends FlatSpec with ShouldMatchers with Mockito {
  
    "A CloudWatchMetricPublisher" should "send one datapoint to CloudWatch" in {
      val cloudwatch = mock[AmazonCloudWatch]
      val metPub = new CloudWatchMetricPublisher(cloudwatch)      
      val metDatum = metPub.sendData("SiteAvailabilityTest", 1)
    
      // make sure that putMetricData was called with any request object
      there was one(cloudwatch).putMetricData(any[PutMetricDataRequest])
    }
}