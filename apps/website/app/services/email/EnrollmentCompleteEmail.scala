package services.email

import models.Celebrity
import models.enums.EmailType
import models.frontend.email._
import _root_.frontend.formatting.DateFormatting.Conversions._
import services.AppConfig
import services.logging.Logging
import services.mail.MailUtils
import services.mail.TransactionalMail

case class EnrollmentCompleteEmail(
  celebrity: Celebrity,
  videoAssetIsDefined: Boolean,
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {

  import EnrollmentCompletedEmail.log

  /**
   * Sends an email to celebalert@egraphs.com to track when celebrities attempt enrollment.
   */
  def send() = {
    val emailStack = EmailViewModel(
      subject = "Enrollment by " + celebrity.publicName,
      fromEmail = "webserver@egraphs.com",
      fromName = "Egraphs",
      toAddresses = List(("stephanie.gene@gmail.com", None))
    )

    val enrollmentCompleteEmailStack = EnrollmentCompleteEmailViewModel(
      celebrityName = celebrity.publicName,
      videoAssetIsDefined = videoAssetIsDefined,
      celebrityEnrollmentStatus = celebrity.enrollmentStatus.name,
      timeEnrolled = celebrity.updated.formatDayAsPlainLanguage
    )

    log("Sending enrollment complete mail to : celebalert@egraphs.com")
    mailService.send(emailStack, MailUtils.getEnrollmentCompleteTemplateContentParts(EmailType.EnrollmentComplete, enrollmentCompleteEmailStack))
  }
}

object EnrollmentCompletedEmail extends Logging