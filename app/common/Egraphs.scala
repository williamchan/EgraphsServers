package common

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import collections.MetricSource
import factory.DBActorFactory
import factory.WebsiteActorFactory
import monitoring.database.DBMonitor
import monitoring.website.WebsiteMonitor
import monitoring.cache.CacheMonitor
import factory.CacheActorFactory

object Egraphs {

  val websiteMonitor = new WebsiteMonitor(cloudWatchClient, 60,
    urlsAndNames, new WebsiteActorFactory)
  val dbMonitor = new DBMonitor(cloudWatchClient, 60, dbNames, new DBActorFactory)
  val cacheMonitor = new CacheMonitor(cloudWatchClient, 60, cacheNames, new CacheActorFactory)

  private def cloudWatchClient: AmazonCloudWatch = {

    val credentials = new BasicAWSCredentials("AKIAIERATN4HQDXFZMJA",
      "C0X33XoddmG7F9T3xALK7NSufU0V7Fv6wxoz6vfs")

    val cloudwatch: AmazonCloudWatch = new AmazonCloudWatchClient(credentials)
    cloudwatch.setEndpoint("monitoring.us-east-1.amazonaws.com")

    cloudwatch
  }

  /**
   * Depending on your metric of interest, create additional MetricSource objects below
   */
  
  private def urlsAndNames: List[MetricSource] = {
    List(MetricSource("https://www.egraphs.com/", "frontPageAvailabilityActor", "frontPage"),
      MetricSource("https://www.egraphs.com/Pedro-Martinez/photos", "photoPageAvailabilityActor", "photoPage"),
      MetricSource("https://www.egraphs.com/about", "staticPageAvailabilityActor", "staticPage"))
  }

  private def dbNames: List[MetricSource] = {
    List(MetricSource("pgpool", "pgpoolDBAvailabilityActor", "pgpool"),
      MetricSource("live", "liveDBAvailabilityActor", "live"))
  }
  
  private def cacheNames: List[MetricSource] = {
    List(MetricSource("redis", "redisAvailabilityActor", "redis"))
  }
}