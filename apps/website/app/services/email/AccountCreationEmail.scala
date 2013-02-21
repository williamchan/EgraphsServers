package services.email

import models.frontend.email._
import models.enums.EmailType
import models.Account
import services.mail.TransactionalMail
import services.logging.Logging
import services.mail.MailUtils
import services.AppConfig
import services.ConsumerApplication
import controllers.routes.WebsiteControllers.getVerifyAccount

case class AccountCreationEmail(
  account: Account, 
  verificationNeeded: Boolean = false, 
  consumerApp: ConsumerApplication = AppConfig.instance[ConsumerApplication],
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {

  import AccountCreationEmail.log
  
  def send() {  
    val emailStack = EmailViewModel(
      subject = "Welcome to Egraphs!",
      fromEmail = "webserver@egraphs.com",
      fromName = "Egraphs",
      toAddresses = List((account.email, None))
    )

    val templateContentParts = if (verificationNeeded) {
      val verifyPasswordUrl = consumerApp.absoluteUrl(getVerifyAccount(account.email, account.resetPasswordKey.get).url)
      MailUtils.getAccountVerificationTemplateContentParts(EmailType.AccountVerification, AccountVerificationEmailViewModel(verifyPasswordUrl))
    } else {
      MailUtils.getAccountConfirmationTemplateContentParts(EmailType.AccountConfirmation)
    }

    log("Sending account creation mail to : " + account.email)    
    mailService.send(emailStack, templateContentParts)  
  }
}

object AccountCreationEmail extends Logging