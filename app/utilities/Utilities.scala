package utilities

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient

object Utilities {

  def getCloudWatchClient: AmazonCloudWatch = {

    val credentials = new BasicAWSCredentials("AKIAIERATN4HQDXFZMJA",
      "C0X33XoddmG7F9T3xALK7NSufU0V7Fv6wxoz6vfs")

    val cloudwatch: AmazonCloudWatch = new AmazonCloudWatchClient(credentials)
    cloudwatch.setEndpoint("monitoring.us-east-1.amazonaws.com")

    cloudwatch

  }
}