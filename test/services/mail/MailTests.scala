package services.mail

import utils.EgraphsUnitTest
import java.util.Properties
import services.Utils
import play.Play

class MailTests extends EgraphsUnitTest
{
  "MailProvider" should "provide the mock instance when mail.smtp value is 'mock'" in {
    val mail = new MailProvider(Map("mail.smtp" -> "mock"), null).get()

    mail.isInstanceOf[MockMail] should be(true)
  }

  it should "provide the gmail instance when smtp value isn't mock and gmail host is provided" in {
    // Set up
    val playConfig = Map(
      "mail.smtp" -> "real",
      "mail.smtp.host" -> "smtp.gmail.com",
      "mail.smtp.user" -> "eboto",
      "mail.smtp.password" -> "herp"
    )
    val utils = new Utils(playConfig)

    // Run test
    val mail = new MailProvider(playConfig, utils).get()

    // Check expectations
    mail should be (Gmail("eboto", "herp"))
  }

  it should "otherwise delegate to Play mail implementation" in {
    val mail = new MailProvider(Map("mail.smtp" -> "herp"), null).get

    mail.isInstanceOf[PlayMailLib] should be (true)
  }

  implicit def mapToProperties(map: Map[String, String]): Properties = {
    import scala.collection.JavaConversions._
    val props = new Properties()

    props.putAll(asJavaMap(map))

    props
  }
}
