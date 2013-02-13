package services.mail

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
import play.api.libs.concurrent.Akka

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
    play.Logger.info("MOCK MAILER: send email")
    play.Logger.info("FROM: " + mailStack.fromEmail)
    play.Logger.info("REPLY-TO: " + mailStack.replyToEmail)

    mailStack.toAddresses.foreach(emailNamePair => play.Logger.info("TO EMAIL: " +
        emailNamePair._1 + ", TO NAME: " + emailNamePair._2.getOrElse("none")))

    play.Logger.info("BCC: " + mailStack.bccAddress.getOrElse("none"))

    // this won't include the header and footer, which are shared across all transactional mail;
    // look at the General template from the Mandrill console to see header/footer html
    templateContentParts.foreach{ case (name, htmlContent) => play.Logger.info("HTML BODY: " + htmlContent) }
  }
}

private[mail] class MandrillTransactionalMail (key: String) extends TransactionalMail {
  override def actionUrl = "https://mandrillapp.com/api/1.0/"

  override def send(mailStack: EmailViewModel, templateContentParts: List[(String, String)]) {
    val methodAndOutputFormat = "messages/send-template.json"

    val jsonIterable = JsonEmailBuilder.sendTemplateJson(mailStack, templateContentParts, key)

    val promiseResponse = WS.url(actionUrl + methodAndOutputFormat).post(jsonIterable)

    promiseResponse.onRedeem {
      response => play.Logger.info("Send-template response: " + response.body)
    }
  }
}