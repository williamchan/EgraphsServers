package services.mail

import collection.JavaConversions._
import utils.EgraphsUnitTest
import services.{Utils, AppConfig}
import play.libs.WS

/**
 * Created with IntelliJ IDEA.
 * User: sbilstein
 * Date: 9/13/12
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
class BulkMailTests extends EgraphsUnitTest {

  val appUtils = AppConfig.instance[Utils]

  "BulkMailProvider" should "provide the mock instance when mail.bulk value is 'mock'" in {
    val mail = new BulkMailProvider(appUtils.properties("mail.bulk" -> "mock"), null).get()

    mail.isInstanceOf[MockBulkMail] should be(true)
  }

  it should "provide the mailchimp instance when the mailchimp information is provided." in {
    val mail = MailchimpProvider
    mail.isInstanceOf[MailChimpBulkMail] should be(true)
  }

  it should "register an email address onto the test list" in {
    val mail = MailchimpProvider
    val emailString =  "test@derp.com"
    mail.subscribeNew(MailchimpProvider.newsletterListId, emailString)

    val response = mail.listMembers(MailchimpProvider.newsletterListId)
    //cleanup
    mailchimpDelete(emailString)
    response.contains("test@derp.com") should be(true)


  }

  private def MailchimpProvider : MailChimpBulkMail = {

    val playConfig = appUtils.properties(
    "mail.bulk" -> "mailchimp",
    "mail.bulk.apikey" -> "2719c3066cc820026cc9ef0f428f2cfa-us5",
    "mail.bulk.datacenter" -> "us5",
    "mail.bulk.newsletterid" -> "cfdd92a4f5"
    )

    val mail = new BulkMailProvider(playConfig, null).get()

    mail.asInstanceOf[MailChimpBulkMail]
  }

  // Private for this test. Will be integrated into BulkMail API when needed.
  private def mailchimpDelete(email: String) = {
    WS.url("https://us5.api.mailchimp.com/1.3/").params(
      Map(
        "output" -> "json",
        "apikey" -> "2719c3066cc820026cc9ef0f428f2cfa-us5",
        "method" -> "listUnsubscribe",
        "id" -> "cfdd92a4f5",
        "email_address" -> email,
        "delete_member" -> "true"
      )
    ).get()
  }


}
