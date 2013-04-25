package services.email

import services.mail.TransactionalMail
import services.AppConfig
import models.frontend.email.EmailViewModel

case class SiteShutdownEmail(
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {
  def send(customerName: String, customerEmail: String, zipUrl: String) {
    val emailHeader = EmailViewModel(
      subject = "Egraphs is shutting down, Download your Egraphs",
      fromEmail = EmailConstants.generalFromEmail,
      fromName = EmailConstants.generalFromName,
      toAddresses = List((customerEmail, Some(customerName)))
    )

   mailService.send(emailHeader, List())
  }
}

