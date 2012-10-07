package services.mail

import play.api.Play.current
import com.typesafe.plugin._
import org.apache.commons.mail.Email
import org.apache.commons.mail.HtmlEmail
import java.util.Properties
import javax.mail.Session
import com.google.inject.{Inject, Provider}
import services.Utils
import services.http.PlayConfig
import collection.mutable.ListBuffer
import play.api.templates.Html
import services.inject.InjectionProvider

/** Interface for sending transactional mails. Transactional mails are  */
trait TransactionalMail {
  def send(mail: HtmlEmail, text: Option[String] = None, html: Option[Html] = None)
  
  private[mail] def toMailerAPI(email: Email): MailerAPI = {
    def addressStringsFromList(addressList: java.util.List[_]): Seq[String] = {
      import scala.collection.JavaConversions._
      for (addresses <- Option(addressList).toSeq; address <- addresses) yield address.toString
    }

    val replyToAddresses = addressStringsFromList(email.getReplyToAddresses)
    val replyTo = if(replyToAddresses.isEmpty) {
      ""
    } else {
      replyToAddresses(0)
    }
    
    // Extract the content into the MailerAPI format
    use[MailerPlugin].email
    .setSubject(email.getSubject)
    .addFrom(email.getFromAddress.getAddress)
    .setReplyTo(replyTo)
    .addRecipient(addressStringsFromList(email.getToAddresses): _*)
    .addCc(addressStringsFromList(email.getCcAddresses): _*)
    .addBcc(addressStringsFromList(email.getBccAddresses): _*)
  }
}

/**
 * Provides a TransactionalMail implementation given the play configuration
 *
 * @param playConfig the Play application's configuration properties
 * @param utils our application utils object
 */
class MailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends InjectionProvider[TransactionalMail]
{
  def get(): TransactionalMail = {
    new DefaultTransactionalMail
  }
}

/**
 * Implementation of the TransactionalMail library that delegates to TypeSafe's Play Plug-in behavior as configured in application.conf.
 * See https://github.com/typesafehub/play-plugins/blob/master/mailer/README.md
 */
private[mail] class DefaultTransactionalMail extends TransactionalMail {
  override def send(mail: HtmlEmail, text: Option[String] = None, html: Option[Html] = None) {
    val mailer = toMailerAPI(mail)
    (text, html) match {
      case (Some(text), Some(html)) => mailer.send(text, html.toString().trim())
      case (Some(text), None) => mailer.send(text)
      case (None, Some(html)) => mailer.sendHtml(html.toString().trim())
      case _ => throw new IllegalStateException("We can't send an email without either text or html in the body.")
    }
  }
} 

