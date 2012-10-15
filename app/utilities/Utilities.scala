package utilities

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import scala.collection.immutable.HashMap

object Utilities {

  def getCloudWatchClient: AmazonCloudWatch = {

    val credentials = new BasicAWSCredentials("AKIAIERATN4HQDXFZMJA",
      "C0X33XoddmG7F9T3xALK7NSufU0V7Fv6wxoz6vfs")

    val cloudwatch: AmazonCloudWatch = new AmazonCloudWatchClient(credentials)
    cloudwatch.setEndpoint("monitoring.us-east-1.amazonaws.com")

    cloudwatch

  }

  def getUrls(historyMap: HashMap[String, List[Int]]): List[String] = {
    historyMap.keySet.toList
  }

  def getRecentHistory(historyMap: HashMap[String, List[Int]]): List[List[Int]] = {
    historyMap.values.toList

  }
}