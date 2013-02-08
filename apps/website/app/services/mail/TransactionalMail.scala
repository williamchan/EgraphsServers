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
import play.api.libs.ws.WS
import play.api.libs.ws.WS.{WSRequest, WSRequestHolder}
import org.joda.time.DateTimeConstants
import play.api.libs.json.{Json, JsValue}
import services.config.ConfigFileProxy

/** Interface for sending transactional mails. Transactional mails are  */
trait TransactionalMail {
  def send(mail: HtmlEmail, text: Option[String] = None, html: Option[Html] = None, templateContentParts: Option[List[(String, String)]] = None)
  protected def newEmail: MailerAPI
  
  private[mail] def toMailerAPI(email: Email): MailerAPI = {
    def addressStringsFromList(addressList: java.util.List[_]): Seq[String] = {
      import scala.collection.JavaConversions._
      for (addresses <- Option(addressList).toSeq; address <- addresses) yield address.toString
    }

    val replyToAddresses = addressStringsFromList(email.getReplyToAddresses)
    val maybeReplyTo = replyToAddresses.headOption

    // Extract the content into the MailerAPI format
    val maybeRepliableEmail = maybeReplyTo.map(address => newEmail.setReplyTo(address.toString))

    maybeRepliableEmail.getOrElse(newEmail)
      .setSubject(email.getSubject)
      .addFrom(email.getFromAddress.toString)
      .addRecipient(addressStringsFromList(email.getToAddresses): _*)
      .addCc(addressStringsFromList(email.getCcAddresses): _*)
      .addBcc(addressStringsFromList(email.getBccAddresses): _*)
  }
}

/**
 * Provides a TransactionalMail implementation given the play configuration
 */
class MailProvider @Inject()(config: ConfigFileProxy) extends InjectionProvider[TransactionalMail]
{
  def get(): TransactionalMail = {
    if (!config.smtpMock) {
      new MandrillTransactionalMail(key = config.smtpOption.get.smtpPassword)
    } else {
      new DefaultTransactionalMail
    }
  }
}

/**
 * Implementation of the TransactionalMail library that delegates to TypeSafe's Play Plug-in behavior as configured in application.conf.
 * See https://github.com/typesafehub/play-plugins/blob/master/mailer/README.md
 */
private[mail] class DefaultTransactionalMail extends TransactionalMail {
  override def send(mail: HtmlEmail, text: Option[String] = None, html: Option[Html] = None, templateContentParts: Option[List[(String, String)]] = None) {
    val mailer = toMailerAPI(mail)
    def performSendMail = (text, html) match {
      case (Some(text), Some(html)) => mailer.send(text, html.toString().trim())
      case (Some(text), None) => mailer.send(text)
      case (None, Some(html)) => mailer.sendHtml(html.toString().trim())
      case _ => throw new IllegalStateException("We can't send an email without either text or html in the body.")
    }
    
    // Figure out why using Akka.future(performSendMail) fails, as per https://egraphs.atlassian.net/browse/SER-421
    performSendMail
  }

  override protected def newEmail: MailerAPI = use[MailerPlugin].email
}

private[mail] class MandrillTransactionalMail (key: String) extends TransactionalMail {
  def actionUrl = "https://mandrillapp.com/api/1.0/"

  override def send(mail: HtmlEmail, text: Option[String] = None, html: Option[Html] = None, templateContentParts: Option[List[(String, String)]] = None) {
    //TODO: think about whether these still need to be Options

    val methodAndOutputFormat = "messages/send-template.json"

    val jsonIterable = getJsonForEmailSend(html.get, text.get, mail, templateContentParts.get)

    val promiseResponse = WS.url(actionUrl + methodAndOutputFormat).
        post(jsonIterable).await(DateTimeConstants.MILLIS_PER_MINUTE).get.body.toString

    println("promiseResponse is " + promiseResponse)
  }

  private def getJsonForEmailSend(html: Html, text: String, mail: HtmlEmail, templateContentParts: List[(String, String)]): JsValue = {
    Json.toJson(Map(
      "key" -> Json.toJson(key),
      "template_name" -> Json.toJson("Account Verification"),
      "template_content" -> Json.toJson(getTemplateContentPieces(templateContentParts)),
      "message" -> Json.toJson(
        Map(
          //"html" -> Json.toJson(html.body),
          //"text" -> Json.toJson("Test email"),
          "subject" -> Json.toJson(mail.getSubject),
          "from_email" -> Json.toJson("webserver@egraphs.com"),
          "from_name" -> Json.toJson("Egraphs"),
          "to" -> Json.toJson(
            Seq(
              Json.toJson(
                Map(
                  "email" -> Json.toJson("stephanie.gene@gmail.com")
                )
              )
            )
          )
        )
      ),
      "async" -> Json.toJson(false)
    ))
  }

  private def getTemplateContentPieces(templateContentPieces: List[(String, String)]): Seq[JsValue] = {
    for ((name, content) <- templateContentPieces) yield {
      Json.toJson(
        Map(
          "name" -> Json.toJson(name),
          "content" -> Json.toJson(content)
        )
      )
    }
  }

  override protected def newEmail: MailerAPI = use[MailerPlugin].email // i don't actually know what this does...
} 

