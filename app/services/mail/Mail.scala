package services.mail

import services.http.PlayConfig
import org.apache.commons.mail.Email
import java.util.Properties
import javax.mail.Session
import com.google.inject.{Inject, Provider}
import services.Utils

import services.http.PlayConfig
import javax.mail.internet.InternetAddress
import collection.mutable.ListBuffer
import collection.JavaConversions

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
        new MockMail(utils)

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
 * Mock implementation of the Mail library. Heavily inspired by Play's implementation in Mail.Mock
 */
private[mail] class MockMail @Inject() (utils: Utils) extends Mail {

  override def send(email: Email) {
    import scala.collection.JavaConversions._

    // Set up to generate email text
    val session = Session.getInstance(utils.properties("mail.smtp.host" -> "myfakesmtpserver.com"))
    email.setMailSession(session)
    email.buildMimeMessage()

    val message = email.getMimeMessage
    message.saveChanges()

    // Extract the content
    def addressStringsFromList(addressList: java.util.List[_]): Iterable[String] = {
      for (addresses <- Option(addressList).toSeq; address <- addresses) yield address.toString
    }
    
    val from = email.getFromAddress.getAddress
    val replyTo = addressStringsFromList(email.getReplyToAddresses)
    val toAddresses = addressStringsFromList(email.getToAddresses)
    val ccs = addressStringsFromList(email.getCcAddresses)
    val bccs = addressStringsFromList(email.getBccAddresses)        
    val subject = email.getSubject
    val body = play.libs.Mail.Mock.getContent(message)

    // Print the content
    var content = new ListBuffer[String]
    
    content += "New message sent via Mock Mailer"
    content += "    From: " + from
    content += "    ReplyTo: " + replyTo.mkString(",")
    content += "    To: " + toAddresses.mkString(", ")
    content += "    Cc: " + ccs.mkString(", ")
    content += "    Bcc: " + bccs.mkString(", ")
    content += "    Subject: " + subject
    content += ""
    content += body
    
    play.Logger.info(content.mkString("\n"))
  }
  
  
}
