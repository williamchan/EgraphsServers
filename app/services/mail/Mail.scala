package services.mail

import services.http.PlayConfig
import org.apache.commons.mail.Email
import java.util.Properties
import javax.mail.Session
import com.google.inject.{Inject, Provider}
import services.Utils

import services.http.PlayConfig

/** Interface for sending e-mail. */
trait Mail {
  def send(mail: Email)
}

/**
 * Provides a Mail implementation given the play configuration
 *
 * @param playConfig the Play application's configuration properties
 * @param utils our application utils object
 */
class MailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends Provider[Mail]
{
  def get(): Mail = {
    val smtp = playConfig.getProperty("mail.smtp")
    val host = playConfig.getProperty("mail.smtp.host")

    (smtp, host) match {
      case ("mock", _) =>
        new MockMail

      case (_, "smtp.gmail.com") =>
        Gmail(utils.requiredConfigurationProperty("mail.smtp.user"),
              utils.requiredConfigurationProperty("mail.smtp.password"))

      case _ =>
        new PlayMailLib
    }
  }
}

/**
 * Implementation of the Mail library that always sends through Gmail, since as of
 * 12/2011 Play can not successfully send mail through gmail.
 */
private[mail] case class Gmail(user: String, password: String) extends Mail
{
  val host = "smtp.gmail.com"

  def send(mail: Email) {
    play.Logger.info("Gmail: sending to " + mail.getToAddresses)
    import scala.collection.JavaConversions._

    // Prepare java mail sessions and transports
    val props = new Properties()
    props.putAll(Map(
      "mail.transport.protocol" -> "smtps",
      "mail.smtps.host" -> host,
      "mail.smtps.auth" -> "true"
    ))

    val session = Session.getDefaultInstance(props)
    mail.setMailSession(session)
    val transport = session.getTransport

    // Prepare message
    mail.buildMimeMessage()
    val mimeMessage = mail.getMimeMessage

    transport.connect(host, 465, user, password)
    Utils.closing(transport) { transport =>
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients)
    }
  }
}


/**
 * Implementation of the Mail library that delegates to Play's behavior as configured in application.conf.
 * See http://www.playframework.org/documentation/1.2.4/configuration#mail for more info.
 */
private[mail] class PlayMailLib extends Mail {
  override def send(mail: Email) = {
    play.libs.Mail.send(mail)
  }
}


/**
 * Implementation of the Mail library that delegates to Play's mock mail implementation
 */
private[mail] class MockMail extends Mail {
  def send(mail: Email) {
    MockPlayMail.sendMail(mail)
  }

  /**Extends Play's built in MockMail to give us access to the send method */
  private[MockMail] object MockPlayMail extends play.libs.Mail.Mock {
    def sendMail(mail: Email) {
      send(mail)
    }
  }
}