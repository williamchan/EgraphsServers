package services

import org.apache.commons.mail.Email
import java.util.Properties
import javax.mail.Session
import play.Play
import Utils.requiredConfigurationProperty

object Mail {
  def send(mail: Email) = {
    mailLib.send(mail)
  }

  private def mailLib: MailLibrary = {
    val useGmail = Play.configuration.getProperty("mail.smtp") != "mock" &&
      host.toLowerCase == "smtp.gmail.com"

    if (useGmail) GmailLib else PlayMailLib
  }

  private lazy val host = requiredConfigurationProperty("mail.smtp.host")
  private lazy val user = requiredConfigurationProperty("mail.smtp.user")
  private lazy val password = requiredConfigurationProperty("mail.smtp.pass")

  private trait MailLibrary {
    def send(mail: Email): Any
  }

  private object GmailLib extends MailLibrary {
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

  private object PlayMailLib extends MailLibrary {
    override def send(mail: Email) = {
      play.libs.Mail.send(mail)
    }
  }
}

