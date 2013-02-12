package services.email

import services.mail.TransactionalMail
import services.logging.Logging
import models.frontend.email._
import services.mail.MailUtils
import models.enums.EmailType
import services.ConsumerApplication
import models.Account
import models.Celebrity

case class CelebrityWelcomeEmail(
  toAddress: String,
  consumerApp: ConsumerApplication,
  celebrity: Celebrity,
  mailService: TransactionalMail,
  bccEmail: Option[String] = None  
) {

  import CelebrityWelcomeEmail.log
  
 /**
  * Sends a welcome email to the celebrities email address with their Egraphs username and a blanked
  * out password field.  We aren't sending the password, it is just a bunch of *****.  The email
  * includes a link to download the latest iPad app.
  */  
  def send() = {
    val emailStack = EmailViewModel(subject = "Welcome to Egraphs!",
                                    fromEmail = "webserver@egraphs.com",
                                    fromName = "Egraphs",
                                    toAddresses = List((toAddress, Some(celebrity.publicName))),
                                    bccEmail)

    val appDownloadLink = consumerApp.getIOSClient(redirectToItmsLink=true).url
    val celebrityWelcomeEmailStack = CelebrityWelcomeEmailViewModel(celebrityName = celebrity.publicName,
                                                                    celebrityEmail = celebrity.account.email,
                                                                    appPlistUrl = appDownloadLink)

    log("Sending celebrity welcome mail to : " + celebrity.account.email + " for celebrity " + celebrity.publicName)
    mailService.send(emailStack, MailUtils.getCelebrityWelcomeTemplateContentParts(EmailType.CelebrityWelcome, celebrityWelcomeEmailStack))      
  }
}

object CelebrityWelcomeEmail extends Logging