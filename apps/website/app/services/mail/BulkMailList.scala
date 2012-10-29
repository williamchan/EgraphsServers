package services.mail

import collection.JavaConversions._
import java.util.concurrent.TimeUnit
import java.util.Properties
import com.google.inject.{Inject, Provider}
import com.google.gson.Gson
import org.joda.time.DateTimeConstants
import play.api.libs.concurrent.Promise
import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.libs.ws.WS.{WSRequest, WSRequestHolder}
import services.inject.InjectionProvider
import services.config.ConfigFileProxy
import services.Time

/**
 * Trait that defines bulk mail lists (e.g. MailChimp, Constant Contact).
 *
 * Bulk mail services manage campaign style mailings like newsletters as opposed to
 * transactional mailings like order confirmations. BulkMail is NOT a replacement for transactional mail services.
 * See the wiki page:  https://egraphs.atlassian.net/wiki/display/DEV/Email
 */
trait BulkMailList {

  /**
   * Subscribes a user to the mailing list.
   *
   * @param email e-mail address of the individual subscribing to the mailing list.
   */
  def subscribeNewAsync(email: String) : Promise[Response]

  /**
   * Throws exceptions if this implementation is not configured correctly.
   *
   * @return returns itself assuming the check passed.
   */
  def checkConfiguration : BulkMailList

  /**
   * List subscribers of the given list.
   * TODO: PLAY20
   * TODO(sbilstein): refactor this when moved to play2.0 to provide a typesafe list using
   * the new JSON API.
   */
  def members : String

  /**
   * Remove a member from the mailing list.
   * @param email The email address of the member being removed.
   */
  def removeMember(email: String) : Promise[Response]

  /**
   * Used to set the value of API calls such as subscribeNew
   *
   * @return ID of newsletter sign up mailing list.
   */
  def newsletterListId : String
}

/**
 * Helper class for configuring BulkMailList implementations
 *
 * @param playConfig the map of application configuration values. See conf/application.conf for values.
 */
class BulkMailListProvider @Inject()(config: ConfigFileProxy) extends InjectionProvider[BulkMailList]
{
  def get() : BulkMailList = {
    //Inspect configuration and return the proper BulkMailList
    val provider = if (config.mailBulkVendor == "mailchimp") {
      MailChimpBulkMailList(
        apikey = config.mailBulkApikey,
        datacenter = config.mailBulkDatacenter,
        newsletterListId = config.mailBulkNewsletterId
      )
    } else {
      StubBulkMailList
    }
    provider.checkConfiguration
  }
}

/**
 * A BulkMailList implementation exclusively for testing and development.
 *
 * @param utils
 */
private[mail] object StubBulkMailList extends BulkMailList
{
  override def subscribeNewAsync(email: String) : Promise[Response] = {
    play.api.Logger.info("Subscribed " + email + " to email list: " + newsletterListId + "\n")
    Promise()
  }
  override def checkConfiguration : BulkMailList = { this }

  override def newsletterListId = "NotARealListId"

  override def members : String = {
    new Gson().toJson(List("derp"))
  }
  
  override def removeMember(email: String) : Promise[Response] = throw new Error("This isn't yet supported on StubBulkMailList")
}

/**
 * BulkMailList implementation that interoperates with the MailChimp API.
 *
 * @param apikey the MailChimp API Key as found in application.conf and
 *     [[https://us5.admin.mailchimp.com/account/api/ the mailchimp account page]]
 * @param datacenter the MailChimp data center used by our account. The correct value for this
 *     appears at the top of the browser when logged in to mailchimp.com and should be set
 *     in the application config.
 * @param newsletterListId the mailing list's newsletter ID as provided by the bulk mail service provider.
 *     the value is probably located in application.conf with the key `mail.bulk.newsletterid`.
 */
private[mail] case class MailChimpBulkMailList (apikey: String, datacenter: String, newsletterListId: String) extends BulkMailList
{
  private def apiUrl = "https://" + datacenter + ".api.mailchimp.com/1.3/"

  override def subscribeNewAsync(email: String) : Promise[Response] = {
    WS.url(apiUrl).withQueryString(
      ("output", "json"),
      ("apikey", apikey),
      ("method", "listSubscribe"),
      ("id", newsletterListId),
      ("email_address", email),
      ("double_optin", "false")
    ).get
  }

  override def checkConfiguration : BulkMailList = {
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

  override def members : String = {
    WS.url(apiUrl).withQueryString(
      ("output", "json"),
      ("apikey", apikey),
      ("method", "listMembers"),
      ("id", newsletterListId)
    ).get.await(10 * DateTimeConstants.MILLIS_PER_SECOND, TimeUnit.MILLISECONDS).get.body.toString
  }

  override def removeMember(email: String) : Promise[Response] = {
    WS.url(apiUrl).post(
      Map(
        "output" -> Seq("json"),
        "apikey" -> Seq(apikey),
        "method" -> Seq("listUnsubscribe"),
        "id" -> Seq(newsletterListId),
        "email_address" -> Seq(email),
        "delete_member" -> Seq("true")
      )
    )
  }
}
