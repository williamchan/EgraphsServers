package services.email

import models.frontend.email._
import models.enums.EmailType
import models.Account
import services.mail.TransactionalMail
import services.logging.Logging
import services.AppConfig
import services.mail.MailUtils
import services.ConsumerApplication
import controllers.routes.WebsiteControllers.getResetPassword

case class ResetPasswordEmail(
  account: Account,
  consumerApp: ConsumerApplication = AppConfig.instance[ConsumerApplication],
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {

  import ResetPasswordEmail.log

  /**
   * Sends an email so that the customer can reset password via the getResetPassword endpoint
   */
  def send() = {
    val emailStack = EmailViewModel(
      subject = "Egraphs Password Recovery",
      fromEmail = EmailUtils.supportFromEmail,
      fromName = EmailUtils.supportFromName,
      toAddresses = List((account.email, None))
    )

    val resetPasswordUrl = consumerApp.absoluteUrl(getResetPassword(account.email, account.resetPasswordKey.get).url)
    val resetPasswordEmailStack = ResetPasswordEmailViewModel(email = account.email,
                                                              resetPasswordUrl = resetPasswordUrl)

    log(s"Sending reset password mail to: ${account.email}")
    mailService.send(emailStack, MailUtils.getResetPasswordTemplateContentParts(
      EmailType.ResetPassword, resetPasswordEmailStack))
  }
}

object ResetPasswordEmail extends Logging