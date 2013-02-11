package services.mail

import scala.collection.JavaConversions._

import com.google.inject.Inject
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.WS
import services.config.ConfigFileProxy
import services.inject.InjectionProvider
import models.frontend.email.EmailViewModel
import org.joda.time.DateTimeConstants

/** Interface for sending transactional mails. Transactional mails are
 *  one-off recipient-specific mail, such as account confirmation, order
 *  confirmation, or view egraph email. We now send transactional mail
 *  through Mandrill. API docs here: https://mandrillapp.com/api/docs/
 */
trait TransactionalMail {
  def actionUrl: String
  def send(mailStack: EmailViewModel, templateContentParts: List[(String, String)])
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
      new StubTransactionalMail
    }
  }
}

private[mail] class StubTransactionalMail extends TransactionalMail {
  override def actionUrl = "#"

  override def send(mailStack: EmailViewModel, templateContentParts: List[(String, String)]) = {
    println("don't forget to update this once we implement replyTo")
    println("may want to do something with txt as well?")

    play.Logger.info("MOCK MAILER: send email")
    play.Logger.info("FROM: " + mailStack.fromEmail)

    mailStack.toAddresses.foreach(emailNamePair => play.Logger.info("TO EMAIL: " +
        emailNamePair._1 + ", TO NAME: " + emailNamePair._2.getOrElse("<none>")))

    play.Logger.info("BCC: " + mailStack.bccAddress.getOrElse("none"))

    templateContentParts.foreach(nameTemplatePair => play.Logger.info("HTML: " + nameTemplatePair._2))
  }
}

private[mail] class MandrillTransactionalMail (key: String) extends TransactionalMail {
  override def actionUrl = "https://mandrillapp.com/api/1.0/"

  override def send(mailStack: EmailViewModel, templateContentParts: List[(String, String)]) {
    val methodAndOutputFormat = "messages/send-template.json"

    val jsonIterable = getJsonForEmailSend(mailStack, templateContentParts)

    // all of this is still unsafe, don't forget to make it better
    val promiseResponse = WS.url(actionUrl + methodAndOutputFormat).
        post(jsonIterable).await(DateTimeConstants.MILLIS_PER_MINUTE).get.body.toString

    println("promiseResponse is " + promiseResponse)
  }

  private def getJsonForEmailSend(mailStack: EmailViewModel, templateContentParts: List[(String, String)]): JsValue = {
    Json.toJson(Map(
      "key" -> Json.toJson(key),
      "template_name" -> Json.toJson("General"),
      "template_content" -> Json.toJson(getTemplateContentPieces(templateContentParts)),
      "message" -> Json.toJson(
        Map(
          "subject" -> Json.toJson(mailStack.subject),
          "from_email" -> Json.toJson(mailStack.fromEmail),
          "from_name" -> Json.toJson(mailStack.fromName),
          "to" -> Json.toJson(getToAddresses(mailStack)),
          "bcc_address" -> Json.toJson(mailStack.bccAddress.getOrElse("")) // TODO: need to make sure nothing weird happens here. test this.
        )
      ),
      "async" -> Json.toJson(true)
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

  private def getToAddresses(mailStack: EmailViewModel): Seq[JsValue] = {
    for ((email, name) <- mailStack.toAddresses) yield {
      Json.toJson(
        Map(
          "email" -> Json.toJson(email),
          "name" -> Json.toJson(name.getOrElse(email)) // use email if no name given
        )
      )
    }
  }
}