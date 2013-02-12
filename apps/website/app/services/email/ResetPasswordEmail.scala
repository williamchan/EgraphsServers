package services.email

import services.mail.TransactionalMail
import services.logging.Logging
import models.frontend.email._
import services.mail.MailUtils
import models.enums.EmailType
import services.ConsumerApplication
import models.Account
import controllers.routes.WebsiteControllers.getResetPassword

case class ResetPasswordEmail(
  account: Account,
  consumerApp: ConsumerApplication,
  mailService: TransactionalMail
) {

  import ResetPasswordEmail.log

  /**
   * Sends an email so that the customer can reset password via the getResetPassword endpoint
   */
  def send() = {
    val emailStack = EmailViewModel(subject = "Egraphs Password Recovery",
                                    fromEmail = "support@egraphs.com",
                                    fromName = "Egraphs Support",
                                    toAddresses = List((account.email, None)))

    val resetPasswordUrl = consumerApp.absoluteUrl(getResetPassword(account.email, account.resetPasswordKey.get).url)
    val resetPasswordEmailStack = ResetPasswordEmailViewModel(email = account.email,
                                                              resetPasswordUrl = resetPasswordUrl)

    log("Sending reset password mail to : " + account.email)
    mailService.send(emailStack, MailUtils.getResetPasswordTemplateContentParts(EmailType.ResetPassword, resetPasswordEmailStack))
  }
}

object ResetPasswordEmail extends Logging