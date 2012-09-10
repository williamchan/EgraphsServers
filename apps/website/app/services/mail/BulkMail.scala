package services.mail

import play.Play.configuration
import com.google.inject.{Inject, Provider}
import services.Utils
import collection.JavaConversions._
import services.http.PlayConfig
import java.util.Properties
import play.libs.WS

/**
 * Trait that defines bulk mail providers (e.g. MailChimp, Constant Contact).
 *
 * Bulk mail services manage campaign style mailings like newsletters as opposed to
 * transactional mailings like order confirmations. BulkMail is NOT a replacement for transactional mail services.
 * See the wiki page:  https://egraphs.atlassian.net/wiki/display/DEV/Email
 */
trait BulkMail {

  /**
   * Subscribes a user to the given mailing list. The id is a key associated with the underlying API.
   *
   * @param listId the mailing list's newsletter ID as provided by the bulk mail service provider.
   *   the value is probably located in application.conf with the key `mail.bulk.newsletterid`.
   * @param email e-mail address of the individual subscribing to the mailing list.
   */
  def subscribeNew(listId: String, email: String)

  /**
   * Throws exceptions if this implementation is not configured correctly.
   *
   * @return returns itself assuming the check passed.
   */
  def checkConfiguration() : BulkMail

  /**
   * Used to set the value of API calls such as subscribeNew
   *
   * @return ID of newsletter sign up mailing list.
   */
  def newsletterListId : String
}

/**
 * Helper class for configuring BulkMail implementations
 *
 * @param playConfig the map of application configuration values. See conf/application.conf for values.
 * @param utils
 */
class BulkMailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends Provider[BulkMail]
{
  def get() : BulkMail = {
    //Inspect properties and return the proper BulkMail
    val provider = if (configuration.getProperty("mail.bulk") == "mailchimp") {
      MailChimpBulkMail
    } else {
      new MockBulkMail(utils)
    }
    provider.checkConfiguration()
  }
}

/**
 * A BulkMail implementation exclusively for testing and development.
 *
 * @param utils
 */
private[mail] case class MockBulkMail (utils: Utils) extends BulkMail
{
  override def subscribeNew(listId: String, email: String) = {
    play.Logger.info("Subscribed " + email + " to email list: " + listId + "\n")
  }
  override def checkConfiguration() : BulkMail = { this }

  override def newsletterListId = "NotARealListId"
}

/**
 * BulkMail implementation that interoperates with the MailChimp API.
 *
 * @param apikey the MailChimp API Key as found in application.conf and
 *     [[https://us5.admin.mailchimp.com/account/api/ the mailchimp account page]]
 * @param datacenter the MailChimp data center used by our account. The correct value for this
 *     appears at the top of the browser when logged in to mailchimp.com and should be set
 *     in the application config.
 */
private[mail] case class MailChimpBulkMail (apikey: String, datacenter: String, newsletterListId: String) extends BulkMail
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

  override def checkConfiguration() : BulkMail = {
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
    this
  }
}

/**
 * Companion object for configuring MailChimpBulkMail
 */
private[mail] object MailChimpBulkMail
  extends MailChimpBulkMail(
    configuration.getProperty("mail.bulk.apikey"),
    configuration.getProperty("mail.bulk.datacenter"),
    configuration.getProperty("mail.bulk.newsletterid")
  )