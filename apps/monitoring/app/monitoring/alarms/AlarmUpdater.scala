package monitoring.alarms

import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.auth.BasicAWSCredentials
import scala.collection.JavaConversions._
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest
import com.amazonaws.services.sns.model.SetTopicAttributesRequest
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sns.model.SubscribeRequest

class AlarmUpdater {

  def updateAlarm(phone: String) = {
    val sns = snsClient
    val arn = "arn:aws:sns:us-east-1:021695013373:SiteUnavailability"

    val subscriptions = sns.listSubscriptionsByTopic(
      new ListSubscriptionsByTopicRequest(arn))
    val listSubscriptions = subscriptions.getSubscriptions

    for (subscription <- listSubscriptions) {
      if (subscription.getProtocol == "sms") {
        sns.unsubscribe(new UnsubscribeRequest(subscription.getSubscriptionArn))
      }
    }
    sns.subscribe(new SubscribeRequest(arn, "sms", phone))
  }

  def snsClient: AmazonSNS = {

    val credentials = new BasicAWSCredentials("AKIAIERATN4HQDXFZMJA",
      "C0X33XoddmG7F9T3xALK7NSufU0V7Fv6wxoz6vfs")
    val sns: AmazonSNS = new AmazonSNSClient(credentials)
    sns.setEndpoint("sns.us-east-1.amazonaws.com")

    sns
  }
}