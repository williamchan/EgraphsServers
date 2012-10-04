package services.mail

import play.api.Play.configuration
import com.google.inject.{Inject, Provider}
import services.Utils
import collection.JavaConversions._
import services.http.PlayConfig
import java.util.Properties
import play.api.libs.ws.WS

/**
 * Trait for defining new bulk mail providers.
 * Bulk mail services manage campaign style mailings like newsletters as opposed to
 * transactional mailings like order confirmations
 */
trait BulkMail {

  /**
   * TODO(sbilstein): Needs commments.
   *
   * @param listId
   * @param email
   */
  def subscribeNew(listId: String, email: String)

  /**
   * TODO(sbilstein): Needs commments.
   */
  def checkConfiguration()
}

/**
 * Helper class for configuring BulkMail implementations
 * @param playConfig
 * @param utils
 */
class BulkMailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends Provider[BulkMail]
{
  def get() : BulkMail = {
    //Inspect properties and return the proper BulkMail
    import play.api.Play.current

    val maybeBulkMail = for {
      bulkMailConfig <- configuration.getString("mail.bulk") if (bulkMailConfig == "mailchimp")
      apikey <- configuration.getString("mail.bulk.apikey")
      datacenter <- configuration.getString("mail.bulk.datacenter")
    } yield {
      MailChimpBulkMail(apikey, datacenter)
    }

    maybeBulkMail.getOrElse(new MockBulkMail(utils))
  }
}

/**
 * A MockMailer for testing purposes
 * @param utils
 */
private[mail] case class MockBulkMail (utils: Utils) extends BulkMail
{
  override def subscribeNew(listId: String, email: String) = {
    play.Logger.info("Subscribed " + email + " to email list: " + listId + "\n")
  }
  override def checkConfiguration() = {}
}

/**
 * Currently a very simple wrapper around one function of the Mailchimp API.
 * @param apikey
 * @param datacenter
 */
private[mail] case class MailChimpBulkMail (apikey: String, datacenter: String) extends BulkMail
{
  override def subscribeNew(listId: String, email: String) = {
    val url = "https://" + datacenter + ".api.mailchimp.com/1.3/"
    val params = Map(
        "output" -> Seq("json"),
        "apikey" -> Seq(apikey),
        "method" -> Seq("listSubscribe"),
        "id" -> Seq(listId),
        "email_address" -> Seq(email),
        "double_optin" -> Seq("false"))
    val responsePromise = WS.url(url).post(params)
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
