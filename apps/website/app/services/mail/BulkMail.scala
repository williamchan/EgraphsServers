package services.mail

import play.Play.configuration
import com.google.inject.{Inject, Provider}
import services.Utils
import collection.JavaConversions._
import services.http.PlayConfig
import java.util.Properties
import play.libs.WS

/**
 * Trait for defining new bulk mail providers.
 * Bulk mail services manage campaign style mailings like newsletters as opposed to
 * transactional mailings like order confirmations. BulkMail is NOT a replacement for transactional mail serrvices.
 * See the wiki page:
 */
trait BulkMail {

  /**
   * Subscribes a user to the given mailing list. The id is a key associated with the underlying API.
   *
   * @param listId
   * @param email
   */
  def subscribeNew(listId: String, email: String)

  /**
   * Throws exceptions if this implementation is not configured correctly.
   * @return returns itself assuming the check passed.
   */
  def checkConfiguration : BulkMail

  /**
   * Used to set the value of API calls such as subscribeNew
   * @return ID of newsletter sign up mailing list.
   */

  def newsletterListId : String
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
    val provider = if (configuration.getProperty("mail.bulk") == "mailchimp") {
      MailChimpBulkMail
    } else {
      new MockBulkMail(utils)
    }
    provider.checkConfiguration
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
  override def checkConfiguration() : BulkMail = { this }

  override def newsletterListId = "NotARealListId"
}

/**
 * Currently a very simple wrapper around one function of the Mailchimp API.
 * @param apikey
 * @param datacenter
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