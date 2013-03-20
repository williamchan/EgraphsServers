package services.email

import models.enums.EmailType
import models.frontend.email._
import services.AppConfig
import services.logging.Logging
import services.mail.MailUtils
import services.mail.TransactionalMail

case class CelebrityRequestEmail(
  celebrityRequestEmailStack: CelebrityRequestEmailViewModel,
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]  
) {

  import CelebrityRequestEmail.log

  def send() {
    val emailStack = EmailViewModel(
      subject = "Request Star Success",
      fromEmail = "webserver@egraphs.com",
      fromName = "Egraphs",
      toAddresses = List((celebrityRequestEmailStack.requesterEmail, None))
    )    

    val celebrityRequestTemplateContentParts = MailUtils.getCelebrityRequestTemplateContentParts(EmailType.CelebrityRequest, celebrityRequestEmailStack)

    log("Sending celebrity request mail to: " + celebrityRequestEmailStack.requesterEmail + " for star: " + celebrityRequestEmailStack.requestedStar)
    mailService.send(emailStack, celebrityRequestTemplateContentParts) 
  }
}

object CelebrityRequestEmail extends Logging