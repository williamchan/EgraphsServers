package jobs

import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import models.{Celebrity, VoiceSample, SignatureSample, EnrollmentBatch}
import libs.Blobs
import Blobs.Conversions._
import services.signature.XyzmoBiometricServices
import db.Schema
import services.voice.{VBGRequest, VBGBiometricServices}

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
      for (batch <- EnrollmentBatchJob.findEnrollmentBatchesPending()) {
        val celebrity = Celebrity.findById(batch.celebrityId).get

        val signatureSamples: List[SignatureSample] = EnrollmentBatchJob.getSignatureSamples(batch)
        val voiceSamples: List[VoiceSample] = EnrollmentBatchJob.getVoiceSamples(batch)

        val isSuccessfulSignatureEnrollment: Boolean = EnrollmentBatchJob.attemptSignatureEnrollment(celebrity, signatureSamples)
        val isSuccessfulVoiceEnrollment: Boolean = EnrollmentBatchJob.attemptVoiceEnrollment(celebrity, voiceSamples)

        val isSuccessfulEnrollment = isSuccessfulSignatureEnrollment && isSuccessfulVoiceEnrollment
        batch.copy(isSuccessfulEnrollment = Some(isSuccessfulEnrollment)).save()
        if (isSuccessfulEnrollment) {
          celebrity.copy(enrollmentStatus = models.Enrolled.value).save()
        }

      }
    }
  }

}

object EnrollmentBatchJob {

  def attemptSignatureEnrollment(celebrity: Celebrity, signatureSamples: scala.List[SignatureSample]): Boolean = {
    for (signatureSample <- signatureSamples) {
      // TODO(wchan): Do this lazily.
      signatureSample.putXyzmoSignatureDataContainerOnBlobstore
    }

    // TODO(wchan): There has to be a better way to do this... contact Xyzmo about user management.
    val xyzmoUID: String = celebrity.getXyzmoUID()
    //    XyzmoBiometricServices.deleteUser(userId = xyzmoUID)
    XyzmoBiometricServices.addUser(userId = xyzmoUID, userName = celebrity.publicName.get)
    XyzmoBiometricServices.addProfile(userId = xyzmoUID, profileName = xyzmoUID)
    val signatureDataContainers = for (signatureSample <- signatureSamples) yield Blobs.get(SignatureSample.getXmlUrl(signatureSample.id)).get.asString
    val isSuccessfulSignatureEnrollment = XyzmoBiometricServices.enrollUser(userId = xyzmoUID, profileName = xyzmoUID, signatureDataContainers = signatureDataContainers)

    println("Result of signature enrollment attempt for celebrity " + celebrity.id.toString + ": " + isSuccessfulSignatureEnrollment.toString)

    isSuccessfulSignatureEnrollment
  }

  private def sendStartEnrollmentRequest(celebrity: Celebrity): VBGRequest = {
    val startEnrollmentRequest = VBGBiometricServices.sendStartEnrollmentRequest(celebrity.id.toString, false)
    if (startEnrollmentRequest.getResponseValue(VBGBiometricServices._errorCode) == "0") {
      // First-time enrollment
      startEnrollmentRequest
    }
    else {
      // Re-enrollment
      VBGBiometricServices.sendStartEnrollmentRequest(celebrity.id.toString, true)
    }
  }

  def attemptVoiceEnrollment(celebrity: Celebrity, voiceSamples: scala.List[VoiceSample]): Boolean = {
    val startEnrollmentRequest = sendStartEnrollmentRequest(celebrity)
    val transactionId = startEnrollmentRequest.getResponseValue(VBGBiometricServices._transactionId)
    //    assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode))
    //    assertNotNull(transactionId)

    for (voiceSample <- voiceSamples) {
      VBGBiometricServices.sendAudioCheckRequest(transactionId, VoiceSample.getWavUrl(voiceSample.id))
      // store metadata on VoiceSample... ignoring errorcodes for now
    }

    val enrollUserRequest = VBGBiometricServices.sendEnrollUserRequest(transactionId)
    val enrollmentSuccessValue = enrollUserRequest.getResponseValue(VBGBiometricServices._success)

    VBGBiometricServices.sendFinishEnrollTransactionRequest(transactionId, enrollmentSuccessValue)

    val isSuccessfulVoiceEnrollment = enrollmentSuccessValue == "true"

    println("Result of voice enrollment attempt for celebrity " + celebrity.id.toString + ": " + isSuccessfulVoiceEnrollment.toString)

    isSuccessfulVoiceEnrollment
  }

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
