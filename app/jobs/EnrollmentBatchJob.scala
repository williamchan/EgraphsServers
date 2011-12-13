package jobs

import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import models.{Celebrity, VoiceSample, SignatureSample, EnrollmentBatch}
import libs.Blobs
import Blobs.Conversions._
import services.signature.XyzmoBiometricServices
import db.{DBSession, Schema}

@On("* * * * * ?") // cron expression: seconds minutes hours day-of-month month day-of-week (year optional)
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

    DBSession.init()
    // TODO(wchan): Should inTransaction be used here?

    //  1. Query for EnrollmentBatches for which isBatchComplete==true and isSuccessfulEnrollment.isNull
    for (batch <- EnrollmentBatchJob.findEnrollmentBatchesPending()) {
      val celebrity = Celebrity.findById(batch.celebrityId).get

      //  2. For each batch, find all SignatureSamples and VoiceSamples via EnrollmentSamples
      val signatureSamples: List[SignatureSample] = EnrollmentBatchJob.getSignatureSamples(batch)
      val voiceSamples: List[VoiceSample] = EnrollmentBatchJob.getVoiceSamples(batch)

      //  3. For SignatureSamples, translate each to SignatureDataContainer and store that on BlobStore… then call enroll method with all SignatureSamples
      for (signatureSample <- signatureSamples) {
        // TODO(wchan): Do this lazily.
        signatureSample.putXyzmoSignatureDataContainerOnBlobstore
      }
      XyzmoBiometricServices.addUser(userId = celebrity.id.toString, userName = celebrity.publicName.get)
      XyzmoBiometricServices.addProfile(userId = celebrity.id.toString, profileName = celebrity.id.toString)
      val signatureDataContainers = for (signatureSample <- signatureSamples) yield Blobs.get(SignatureSample.getXmlUrl(signatureSample.id)).get.asString
      val isSuccessfulSignatureEnrollment = XyzmoBiometricServices.enrollUser(userId = celebrity.id.toString, profileName = celebrity.id.toString, signatureDataContainers = signatureDataContainers)

      //  4. For VoiceSamples, call enroll method
      for (voiceSample <- voiceSamples) {
        voiceSample

      }
      val isSuccessfulVoiceEnrollment = false

      //  5. Update EnrollmentBatch and Celebrity
      val isSuccessfulEnrollment = isSuccessfulSignatureEnrollment && isSuccessfulVoiceEnrollment
      batch.copy(isSuccessfulEnrollment = Some(isSuccessfulEnrollment)).save()
      if (isSuccessfulEnrollment) {
        celebrity.copy(enrollmentStatus = models.Enrolled.value).save()
      }

    }
    DBSession.commit()
  }

}

object EnrollmentBatchJob {

  def findEnrollmentBatchesPending(): List[EnrollmentBatch] = {
    from(Schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.isBatchComplete === true and enrollmentBatch.isSuccessfulEnrollment.isNull)
        select (enrollmentBatch)
    ).toList
  }

  def getSignatureSamples(enrollmentBatch: EnrollmentBatch): List[SignatureSample] = {
    from(Schema.signatureSamples, Schema.enrollmentSamples)((signatureSample, enrollmentSample) =>
      where(enrollmentSample.enrollmentBatchId === enrollmentBatch.id and enrollmentSample.signatureSampleId === signatureSample.id)
        select (signatureSample)
    ).toList
  }

  def getVoiceSamples(enrollmentBatch: EnrollmentBatch): List[VoiceSample] = {
    from(Schema.voiceSamples, Schema.enrollmentSamples)((voiceSample, enrollmentSample) =>
      where(enrollmentSample.enrollmentBatchId === enrollmentBatch.id and enrollmentSample.voiceSampleId === voiceSample.id)
        select (voiceSample)
    ).toList
  }

}