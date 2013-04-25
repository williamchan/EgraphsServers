package services.email

import services.mail.{MailUtils, TransactionalMail}
import services.AppConfig
import models.frontend.email.EmailViewModel
import services.logging.Logging

case class SiteShutdownEmail(
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {
  def send(customerName: String, customerEmail: String, zipUrl: String) {
    val emailHeader = EmailViewModel(
      subject = "Important Egraphs Announcement - Action Required",
      fromEmail = EmailConstants.generalFromEmail,
      fromName = EmailConstants.generalFromName,
      toAddresses = List((customerEmail, Some(customerName)))
    )
    val emailParts = MailUtils.baseList("Important Egraphs Announcement, Action Required") :::
      List(("egraphs_announcement", views.html.frontend.email.site_shutdown(customerName, zipUrl).body))
     mailService.send(emailHeader, emailParts, views.html.frontend.email.site_shutdown(customerName, zipUrl).body)
  }
}

object SiteShutdownEmail extends Logging