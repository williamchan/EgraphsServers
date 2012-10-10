package history

import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.Dimension

import java.util.Date

object WebsiteHistory {

  private val ONE_HOUR_IN_MILLISECONDS = 3600000
  private val THREE_MINUTES_IN_MILLISECONDS = 180000

  def getHistory(cloudwatch: AmazonCloudWatch, lastMinutes: Int): GetMetricStatisticsResult = {

    val getRequest = new GetMetricStatisticsRequest()
      .withStartTime(new Date(new Date().getTime() - ONE_HOUR_IN_MILLISECONDS))
      .withNamespace("SiteAvailability")
      .withPeriod(60 * lastMinutes) // i suppose this is the number of minutes?
      .withDimensions(new Dimension().withName("Availability").withValue("m1.small"))
      .withMetricName("SiteAvailability")
      .withStatistics("Average", "SampleCount")
      .withEndTime(new Date())

    cloudwatch.getMetricStatistics(getRequest)
  }
}