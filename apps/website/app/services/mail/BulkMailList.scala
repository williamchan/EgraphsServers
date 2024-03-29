package services.mail

import java.util.concurrent.TimeUnit
import scala.concurrent._
import scala.concurrent.duration._
import com.google.inject.{Inject, Provider}
import com.google.gson.Gson
import org.joda.time.DateTimeConstants
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws._
import play.api.libs.ws.WS._
import play.api.libs.json.Json
import services.inject.InjectionProvider
import services.config.ConfigFileProxy

/**
 * Trait that defines bulk mail lists (e.g. MailChimp, Constant Contact).
 *
 * Bulk mail services manage campaign style mailings like newsletters as opposed to
 * transactional mailings like order confirmations. BulkMail is NOT a replacement for transactional mail services.
 * See the wiki page:  https://egraphs.atlassian.net/wiki/display/DEV/Email
 */
trait BulkMailList {

  def apikey : String
  /**
   * Subscribes a user to the mailing list.
   *
   * @param email e-mail address of the individual subscribing to the mailing list.
   */
  def subscribeNewAsync(email: String) : Future[Response]

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
  def removeMember(email: String) : Future[Response]

  /**
   * Used to set the value of API calls such as subscribeNew
   *
   * @return ID of newsletter sign up mailing list.
   */
  def newsletterListId : String

  /**
   * Returns the endpoint url for AJAX services.
   */
  def actionUrl : String

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
  override def apikey = "derp"

  override def actionUrl = "#"


  override def subscribeNewAsync(email: String) : Future[Response] = {
    play.api.Logger.info("Subscribed " + email + " to email list: " + newsletterListId)
    Future(throw new Exception("Oops, didn't think you'd use this response in subscribeNewAsync."))
  }

  override def checkConfiguration : BulkMailList = { this }

  override def newsletterListId = "NotARealListId"

  override def members : String = {
    new Gson().toJson(List("derp"))
  }

  override def removeMember(email: String) : Future[Response] = {
    play.api.Logger.info("Unsubscribed " + email + " from email list: " + newsletterListId)
    Future(throw new Exception("Oops, didn't think you'd use this response in removeMember."))
  }
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
  override def actionUrl = "https://" + datacenter + ".api.mailchimp.com/1.3/"

  override def subscribeNewAsync(email: String) : Future[Response] = {
    WS.url(actionUrl).withQueryString(
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
    val futureResponse = WS.url(actionUrl).withQueryString(
      ("output", "json"),
      ("apikey", apikey),
      ("method", "listMembers"),
      ("id", newsletterListId)
    ).get

    Await.result(futureResponse, 10 seconds).body.toString
  }

  override def removeMember(email: String) : Future[Response] = {
    WS.url(actionUrl).post(
      Json.obj(
        "output" -> Json.arr("json"),
        "apikey" -> Json.arr(apikey),
        "method" -> Json.arr("listUnsubscribe"),
        "id" -> Json.arr(newsletterListId),
        "email_address" -> Json.arr(email),
        "delete_member" -> Json.arr("true")
      )
    )
  }
}
