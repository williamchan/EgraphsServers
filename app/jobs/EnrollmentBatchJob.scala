package jobs

import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import services.blobs.Blobs
import Blobs.Conversions._
import services.signature.XyzmoBiometricServices
import services.db.Schema
import models._
import services.AppConfig
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

        val signatureSamples: List[SignatureSample] = EnrollmentBatchJob.getSignatureSamples(batch)
        val voiceSamples: List[VoiceSample] = EnrollmentBatchJob.getVoiceSamples(batch)

        val isSuccessfulSignatureEnrollment: Boolean = EnrollmentBatchJob.attemptSignatureEnrollment(batch, signatureSamples, celebrity)
        val voiceEnrollmentResult: Either[VoiceBiometricsError, Boolean] = new services.voice.VBGVoiceBiometricService().enroll(batch, voiceSamples) // todo(wchan): get VoiceBiometricService via injection

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
  val blobs = AppConfig.instance[Blobs]
  val schema = AppConfig.instance[Schema]

  def attemptSignatureEnrollment(enrollmentBatch: EnrollmentBatch, signatureSamples: scala.List[SignatureSample], celebrity: Celebrity): Boolean = {
    for (signatureSample <- signatureSamples) {
      // TODO(wchan): Do this lazily.
      signatureSample.putXyzmoSignatureDataContainerOnBlobstore
    }

    // TODO(wchan): There has to be a better way to do this... contact Xyzmo about user management.
    val xyzmoUID: String = celebrity.getXyzmoUID()
    //    XyzmoBiometricServices.deleteUser(userId = xyzmoUID)
    XyzmoBiometricServices.addUser(userId = xyzmoUID, userName = celebrity.publicName.get)
    XyzmoBiometricServices.addProfile(userId = xyzmoUID, profileName = xyzmoUID)
    val signatureDataContainers = for (signatureSample <- signatureSamples) yield blobs.get(SignatureSample.getXmlUrl(signatureSample.id)).get.asString
    val xyzmoEnrollDynamicProfile = XyzmoBiometricServices.enrollUser(userId = xyzmoUID, profileName = xyzmoUID, signatureDataContainers = signatureDataContainers)
    val isSuccessfulSignatureEnrollment = xyzmoEnrollDynamicProfile.isSuccessfulSignatureEnrollment || xyzmoEnrollDynamicProfile.isProfileAlreadyEnrolled

    println("Result of signature enrollment attempt for celebrity " + celebrity.id.toString + ": " + isSuccessfulSignatureEnrollment.toString)

    isSuccessfulSignatureEnrollment
  }

  def findEnrollmentBatchesPending(): List[EnrollmentBatch] = {
    from(schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.isBatchComplete === true and enrollmentBatch.isSuccessfulEnrollment.isNull)
        select (enrollmentBatch)
    ).toList
  }

  def getSignatureSamples(enrollmentBatch: EnrollmentBatch): List[SignatureSample] = {
    from(schema.signatureSamples, schema.enrollmentSamples)((signatureSample, enrollmentSample) =>
      where(enrollmentSample.enrollmentBatchId === enrollmentBatch.id and enrollmentSample.signatureSampleId === signatureSample.id)
        select (signatureSample)
    ).toList
  }

  def getVoiceSamples(enrollmentBatch: EnrollmentBatch): List[VoiceSample] = {
    from(schema.voiceSamples, schema.enrollmentSamples)((voiceSample, enrollmentSample) =>
      where(enrollmentSample.enrollmentBatchId === enrollmentBatch.id and enrollmentSample.voiceSampleId === voiceSample.id)
        select (voiceSample)
    ).toList
  }

}
