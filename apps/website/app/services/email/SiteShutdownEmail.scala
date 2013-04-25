package services.email

import services.mail.TransactionalMail
import services.AppConfig
import models.frontend.email.EmailViewModel

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
    val emailParts = List(("title", "<title>Important Egraphs Announcement, Action Required</title>"),
    ("Egraphs Announcement", views.html.frontend.email.site_shutdown(customerName, zipUrl).body))
     mailService.send(emailHeader, emailParts)
  }
}

