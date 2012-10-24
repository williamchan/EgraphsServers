package common

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import monitoring.website.WebsiteMonitor
import monitoring.database.DBMonitor

object Egraphs {

  val websiteMonitor = new WebsiteMonitor(cloudWatchClient, 60, urlsAndNames)
  val dbMonitor = new DBMonitor(cloudWatchClient, 10, dbNames)

  private def cloudWatchClient: AmazonCloudWatch = {

    val credentials = new BasicAWSCredentials("AKIAIERATN4HQDXFZMJA",
      "C0X33XoddmG7F9T3xALK7NSufU0V7Fv6wxoz6vfs")

    val cloudwatch: AmazonCloudWatch = new AmazonCloudWatchClient(credentials)
    cloudwatch.setEndpoint("monitoring.us-east-1.amazonaws.com")

    cloudwatch
  }

  private def urlsAndNames: List[(String, String, String)] = {
    // add new URL/actor/friendly name pairs to check here
    List(("https://www.egraphs.com/", "frontPageAvailabilityActor", "frontPage"),
      ("https://www.egraphs.com/Pedro-Martinez/photos", "photoPageAvailabilityActor", "photoPage"),
      ("https://www.egraphs.com/about", "staticPageAvailabilityActor", "staticPage"))
  }

  private def dbNames: List[(String, String, String)] = {
    List(("default", "liveDBAvailabilityActor", "live"),
      ("replica", "replicaDBAvailabilityActor", "replica"))
  }
}