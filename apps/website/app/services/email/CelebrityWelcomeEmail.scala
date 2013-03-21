package services.email

import models.frontend.email._
import models.enums.EmailType
import models.Account
import models.Celebrity
import services.mail.TransactionalMail
import services.logging.Logging
import services.AppConfig
import services.mail.MailUtils
import services.ConsumerApplication

case class CelebrityWelcomeEmail(
  toAddress: String,
  consumerApp: ConsumerApplication,
  celebrity: Celebrity,
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {

  import CelebrityWelcomeEmail.log
  
 /**
  * Sends a welcome email to the celebrities email address with their Egraphs username and a blanked
  * out password field.  We aren't sending the password, it is just a bunch of *****.  The email
  * includes a link to download the latest iPad app.
  */  
  def send() = {
    val emailStack = EmailViewModel(
      subject = "Welcome to Egraphs!",
      fromEmail = "webserver@egraphs.com",
      fromName = "Egraphs",
      toAddresses = List((toAddress, Some(celebrity.publicName)))
    )

    val appDownloadLink = consumerApp.getIOSClient(redirectToItmsLink=true).url
    val celebrityWelcomeEmailStack = CelebrityWelcomeEmailViewModel(celebrityName = celebrity.publicName,
                                                                    celebrityEmail = celebrity.account.email,
                                                                    appPlistUrl = appDownloadLink)

    log("Sending celebrity welcome mail to : " + celebrity.account.email + " for celebrity " + celebrity.publicName)
    mailService.send(emailStack, MailUtils.getCelebrityWelcomeTemplateContentParts(EmailType.CelebrityWelcome, celebrityWelcomeEmailStack))      
  }
}

object CelebrityWelcomeEmail extends Logging