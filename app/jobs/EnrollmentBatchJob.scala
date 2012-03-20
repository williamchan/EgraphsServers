package jobs

import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import services.db.Schema
import models._
import services.AppConfig
import services.signature.SignatureBiometricsError
import services.voice.VoiceBiometricsError

@On("0 /10 * * * ?") // cron expression: seconds minutes hours day-of-month month day-of-week (year optional)
class EnrollmentBatchJob extends Job {
  //  Setup process to query for EnrollmentBatches that are {isBatchComplete = true and isSuccessfulEnrollment = null},
  //  and attempt enrollment. Failed enrollment changes Celebrity.enrollmentStatus to "NotEnrolled",
  //  whereas successful enrollment changes Celebrity.enrollmentStatus to "Enrolled".
  //
  //  1. Query for EnrollmentBatches for which isBatchComplete==true and isSuccessfulEnrollment.isNull
  //  2. For each EnrollmentBatch, call enrollSignature and enrollVoice
  //  5. Update EnrollmentBatch and Celebrity with results
  override def doJob() {
    inTransaction {
      val celebStore = AppConfig.instance[CelebrityStore]

      val pendingEnrollmentBatches: List[EnrollmentBatch] = EnrollmentBatchJob.findEnrollmentBatchesPending()
      for (enrollmentBatch <- pendingEnrollmentBatches) {
        val signatureEnrollmentResult: Either[SignatureBiometricsError, Boolean] = enrollmentBatch.enrollSignature
        val isSuccessfulSignatureEnrollment: Boolean = if (signatureEnrollmentResult.isRight) {
          signatureEnrollmentResult.right.get
        } else {
          false
        }

        val voiceEnrollmentResult: Either[VoiceBiometricsError, Boolean] = enrollmentBatch.enrollVoice
        val isSuccessfulVoiceEnrollment: Boolean = if (voiceEnrollmentResult.isRight) {
          voiceEnrollmentResult.right.get
        } else {
          false
        }

        val isSuccessfulEnrollment = isSuccessfulSignatureEnrollment && isSuccessfulVoiceEnrollment
        enrollmentBatch.copy(isSuccessfulEnrollment = Some(isSuccessfulEnrollment)).save()

        val celebrity = celebStore.findById(enrollmentBatch.celebrityId).get
        if (isSuccessfulEnrollment) {
          celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
        } else {
          celebrity.withEnrollmentStatus(EnrollmentStatus.FailedEnrollment).save()
        }
      }
    }
  }
}

object EnrollmentBatchJob {
  val schema = AppConfig.instance[Schema]

  def findEnrollmentBatchesPending(): List[EnrollmentBatch] = {
    from(schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.isBatchComplete === true and enrollmentBatch.isSuccessfulEnrollment.isNull)
        select (enrollmentBatch)
    ).toList
  }
}
