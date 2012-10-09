package monitoring.website

import akka.actor._
import play.libs.Akka
import akka.util.duration._
import scala.collection.mutable.HashMap
import common.CloudWatchMetricPublisher

object WebsiteMonitoring {
  
  val map = new HashMap[String, String]

  def init() = {
    getURLs
    scheduleJob
  }
  
  private def getURLs = {
    
    // NOT CURRENTLY USED, ASK MYYK
    
    // add additional URLs here
    map += "frontPageURL" -> "https://www.egraphs.com/"
    map += "photoPageURL" -> "https://www.egraphs.com/Pedro-Martinez/photos"
    map += "staticPageURL" -> "https://www.egraphs.com/about"
    
  }

  private def scheduleJob() = {

    val frontPageURL = "https://www.egraphs.com/"
    val photoPageURL = "https://www.egraphs.com/Pedro-Martinez/photos"
    val staticPageURL = "https://www.egraphs.com/about"

    val myFrontPageActor = Akka.system.actorOf(
      Props(new WebsiteAvailabilityActor(frontPageURL, new CloudWatchMetricPublisher)), 
      name = "frontPageAvailabilityActor")
    Akka.system.scheduler.schedule(0 seconds, 1 minute, myFrontPageActor, CheckStatus)

    val myPhotoPageActor = Akka.system.actorOf(
      Props(new WebsiteAvailabilityActor(photoPageURL, new CloudWatchMetricPublisher)), 
      name = "photoPageAvailabilityActor")
    Akka.system.scheduler.schedule(0 seconds, 1 minute, myPhotoPageActor, CheckStatus)

    val myStaticPageActor = Akka.system.actorOf(
      Props(new WebsiteAvailabilityActor(staticPageURL, new CloudWatchMetricPublisher)), 
      name = "staticPageeAvailabilityActor")
    Akka.system.scheduler.schedule(0 seconds, 1 minute, myStaticPageActor, CheckStatus)
  }
}