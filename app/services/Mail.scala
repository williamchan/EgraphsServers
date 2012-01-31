package services

import org.apache.commons.mail.Email
import java.util.Properties
import javax.mail.Session
import play.Play
import Utils.requiredConfigurationProperty
import com.google.inject.Provider

trait Mail {
  def send(mail: Email): Any
}

object Mail {
  private lazy val host = requiredConfigurationProperty("mail.smtp.host")
  private lazy val user = requiredConfigurationProperty("mail.smtp.user")
  private lazy val password = requiredConfigurationProperty("mail.smtp.pass")

  /**
   * Provides Guice with the correct Mail implementation for a given configuration
   * in application.conf
   */
  object MailProvider extends Provider[Mail] {
    def get(): Mail = {
      val useGmail = Play.configuration.getProperty("mail.smtp") != "mock" &&
        host.toLowerCase == "smtp.gmail.com"

      if (useGmail) new Gmail(user, password) else new PlayMailLib
    }
  }

  /**
   * Implementation of the Mail library that always sends through Gmail, since as of
   * 12/2011 Play can not successfully send mail through gmail.
   */
  private[Mail] class Gmail(user: String, password: String) extends Mail {
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
  private[Mail] class PlayMailLib extends Mail {
    override def send(mail: Email) = {
      play.libs.Mail.send(mail)
    }
  }
}


