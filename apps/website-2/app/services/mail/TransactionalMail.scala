package services.mail

import play.api.Play.current
import com.typesafe.plugin.use
import com.typesafe.plugin.MailerAPI
import com.typesafe.plugin.MailerPlugin
import org.apache.commons.mail.Email
import org.apache.commons.mail.HtmlEmail
import com.google.inject.{Inject, Provider}
import services.Utils
import collection.mutable.ListBuffer
import play.api.templates.Html
import scala.collection.JavaConversions._
import services.inject.InjectionProvider
import play.api.libs.concurrent.Akka

/** Interface for sending transactional mails. Transactional mails are  */
trait TransactionalMail {
  def send(mail: HtmlEmail, text: Option[String] = None, html: Option[Html] = None)
  protected def newEmail: MailerAPI
  
  private[mail] def toMailerAPI(email: Email): MailerAPI = {
    def addressStringsFromList(addressList: java.util.List[_]): Seq[String] = {
      import scala.collection.JavaConversions._
      for (addresses <- Option(addressList).toSeq; address <- addresses) yield address.toString
    }

    val replyToAddresses = addressStringsFromList(email.getReplyToAddresses)
    val maybeReplyTo = replyToAddresses.headOption

    // Extract the content into the MailerAPI format
    val maybeRepliableEmail = maybeReplyTo.map(address => newEmail.setReplyTo(address))

    maybeRepliableEmail.getOrElse(newEmail)
      .setSubject(email.getSubject)
      .addFrom(email.getFromAddress.getAddress)
      .addRecipient(addressStringsFromList(email.getToAddresses): _*)
      .addCc(addressStringsFromList(email.getCcAddresses): _*)
      .addBcc(addressStringsFromList(email.getBccAddresses): _*)
  }
}

/**
 * Provides a TransactionalMail implementation given the play configuration
 */
class MailProvider @Inject() extends InjectionProvider[TransactionalMail]
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
    def performSendMail = (text, html) match {
      case (Some(text), Some(html)) => mailer.send(text, html.toString().trim())
      case (Some(text), None) => mailer.send(text)
      case (None, Some(html)) => mailer.sendHtml(html.toString().trim())
      case _ => throw new IllegalStateException("We can't send an email without either text or html in the body.")
    }
    
    performSendMail
  }

  override protected def newEmail: MailerAPI = use[MailerPlugin].email
} 

