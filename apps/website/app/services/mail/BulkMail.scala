package services.mail

import play.Play.configuration
import com.google.inject.{Inject, Provider}
import services.Utils
import collection.JavaConversions._
import services.http.PlayConfig
import java.util.Properties
import play.libs.WS


trait BulkMail {
  def subscribeNew(listId: String, email: String)
  def checkConfiguration()
}

class BulkMailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends Provider[BulkMail]
{
  def get() : BulkMail = {
    //Inspect properties and return the proper BulkMail
    if (configuration.getProperty("mail.bulk") == "mailchimp") {
      MailChimpBulkMail
    } else {
      new MockBulkMail(utils)
    }
  }
}
private[mail] case class MockBulkMail (utils: Utils) extends BulkMail
{
  override def subscribeNew(listId: String, email: String) = {
    play.Logger.info("Subscribed " + email + " to email list: " + listId + "\n")
  }
  override def checkConfiguration() = {}
}

private[mail] case class MailChimpBulkMail (apikey: String, datacenter: String) extends BulkMail
{
  override def subscribeNew(listId: String, email: String) = {
  val url = "https://" + datacenter + ".api.mailchimp.com/1.3/"
  val responsePromise = WS.url(url).params(
    Map(
      "output" -> "json",
      "apikey" -> apikey,
      "method" -> "listSubscribe",
      "id" -> listId,
      "email_address" -> email,
      "double_optin" -> "false"
    )
  ).getAsync()

  }

  override def checkConfiguration() = {
    require(
      apikey != null,
      """
      application.conf: A "mail.bulk.apikey" configuration must be provided.
      """
    )
    require(
      datacenter != null,
      """
      application.conf: A "mail.bulk.datacenter" configuration must be provided.
      """
    )


  }
}

private[mail] object MailChimpBulkMail
  extends MailChimpBulkMail(configuration.getProperty("mail.bulk.apikey"), configuration.getProperty("mail.bulk.datacenter"))