package services.mail

import collection.JavaConversions._
import utils.EgraphsUnitTest
import services.{Utils, AppConfig}
import play.api.libs.ws.WS
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import services.config.ConfigFileProxy
import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class BulkMailListTests extends EgraphsUnitTest {

  val apiKey = "2719c3066cc820026cc9ef0f428f2cfa-us5"
  val newsletterId = "cfdd92a4f5"
  val datacenter = "us5"
  
  lazy val appUtils = AppConfig.instance[Utils]

  "BulkMailListProvider" should "provide the mock instance when mail.bulk value is 'mock'" in new EgraphsTestApplication {
    val mockConfig = mock[ConfigFileProxy]
    mockConfig.mailBulkVendor returns "mock"

    val mailList = new BulkMailListProvider(mockConfig).get()

    mailList should be(StubBulkMailList)
  }

  it should "provide the mailchimp instance when the mailchimp information is provided." in new EgraphsTestApplication {
    val mailList = mailchimpProvider
    mailList.isInstanceOf[MailChimpBulkMailList] should be(true)
  }

  it should "register an email address onto the test list" in new EgraphsTestApplication {
    val mailList = mailchimpProvider
    val email = "test@derp.com"
    val future = mailList.subscribeNewAsync(email)
    future.await(10, TimeUnit.SECONDS)

    val response = mailList.members

    //cleanup
    mailList.removeMember(email)
    response.contains(email) should be(true)
  }

  private def mailchimpProvider : MailChimpBulkMailList = {
    val mockConfig = mock[ConfigFileProxy]
    mockConfig.mailBulkVendor returns "mailchimp"
    mockConfig.mailBulkApikey returns apiKey
    mockConfig.mailBulkDatacenter returns datacenter
    mockConfig.mailBulkNewsletterId returns newsletterId

    val mail = new BulkMailListProvider(mockConfig).get()

    mail.asInstanceOf[MailChimpBulkMailList]
  }
}
