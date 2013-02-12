package services.email

import services.mail.TransactionalMail
import services.logging.Logging
import models.frontend.email._
import services.mail.MailUtils
import models.enums.EmailType
import services.ConsumerApplication
import models.Account
import controllers.routes.WebsiteControllers.getVerifyAccount

case class AccountCreationEmail(
  account: Account, 
  verificationNeeded: Boolean = false, 
  consumerApp: ConsumerApplication,
  mailService: TransactionalMail
) {

  import AccountCreationEmail.log
  
  def send() {  
    val emailStack = EmailViewModel(subject = "Welcome to Egraphs!",
                                    fromEmail = "webserver@egraphs.com",
                                    fromName = "Egraphs",
                                    toAddresses = List((account.email, None)))

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