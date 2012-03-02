package jobs

import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import services.db.Schema
import models._
import services.AppConfig
import services.signature.SignatureBiometricsError
import services.voice.VoiceBiometricsError

@On("0 0 0 * * ?") // cron expression: seconds minutes hours day-of-month month day-of-week (year optional)
class EnrollmentBatchJob extends Job {
  //  Setup process to query for EnrollmentBatches that are {isBatchComplete = true and isSuccessfulEnrollment = null},
  //  and attempt enrollment. Failed enrollment changes Celebrity.enrollmentStatus to "NotEnrolled",
  //  whereas successful enrollment changes Celebrity.enrollmentStatus to "Enrolled".
  //
  //  1. Query for EnrollmentBatches for which isBatchComplete==true and isSuccessfulEnrollment.isNull
  //  2. For each, find all SignatureSamples and VoiceSamples via EnrollmentSamples
  //  3. For SignatureSamples, translate each to SignatureDataContainer and store that on BlobStore… then call enroll method with all SignatureSamples
  //  4. For VoiceSamples, call enroll method
  //  5. Update EnrollmentBatch and Celebrity
  override def doJob() {
    // TODO(wchan): Should inTransaction be used here?

    inTransaction {
      val celebStore = AppConfig.instance[CelebrityStore]

      for (batch <- EnrollmentBatchJob.findEnrollmentBatchesPending()) {
        val celebrity = celebStore.findById(batch.celebrityId).get

        val signatureEnrollmentResult: Either[SignatureBiometricsError, Boolean] = batch.enrollSignature
        val isSuccessfulSignatureEnrollment: Boolean = if (signatureEnrollmentResult.isRight) {
          signatureEnrollmentResult.right.get
        } else {
          false
        }

        val voiceEnrollmentResult: Either[VoiceBiometricsError, Boolean] = batch.enrollVoice
        val isSuccessfulVoiceEnrollment: Boolean = if (voiceEnrollmentResult.isRight) {
          voiceEnrollmentResult.right.get
        } else {
          false
        }

        val isSuccessfulEnrollment = isSuccessfulSignatureEnrollment && isSuccessfulVoiceEnrollment
        batch.copy(isSuccessfulEnrollment = Some(isSuccessfulEnrollment)).save()
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
