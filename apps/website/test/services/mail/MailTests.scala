package services.mail

import utils.EgraphsUnitTest
import services.{AppConfig, Utils}

class MailTests extends EgraphsUnitTest
{
  val appUtils = AppConfig.instance[Utils]

  "MailProvider" should "provide the mock instance when mail.smtp value is 'mock'" in {
    val mail = new MailProvider(appUtils.properties("mail.smtp" -> "mock"), null).get()

    mail.isInstanceOf[MockTransactionalMail] should be(true)
  }

  it should "provide the gmail instance when smtp value isn't mock and gmail host is provided" in {
    // Set up
    val playConfig = appUtils.properties(
      "mail.smtp" -> "real",
      "mail.smtp.host" -> "smtp.gmail.com",
      "mail.smtp.user" -> "eboto",
      "mail.smtp.pass" -> "herp"
    )
    val utils = new Utils(playConfig)

    // Run test
    val mail = new MailProvider(playConfig, utils).get()

    // Check expectations
    mail should be (Gmail("eboto", "herp"))
  }

  it should "otherwise delegate to Play mail implementation" in {
    val mail = new MailProvider(appUtils.properties("mail.smtp" -> "herp"), null).get()

    mail.isInstanceOf[PlayTransactionalMailLib] should be (true)
  }
}
