package jobs

import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import services.blobs.Blobs
import Blobs.Conversions._
import services.signature.TestXyzmoBiometricServices
import services.db.Schema
import services.voice.{VBGRequest, VBGDevRandomNumberBiometricServices}
import models._
import services.AppConfig

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

        val isSuccessfulSignatureEnrollment: Boolean = EnrollmentBatchJob.attemptSignatureEnrollment(celebrity, signatureSamples)
        val isSuccessfulVoiceEnrollment: Boolean = EnrollmentBatchJob.attemptVoiceEnrollment(celebrity, voiceSamples)

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

  def attemptSignatureEnrollment(celebrity: Celebrity, signatureSamples: scala.List[SignatureSample]): Boolean = {
    for (signatureSample <- signatureSamples) {
      // TODO(wchan): Do this lazily.
      signatureSample.putXyzmoSignatureDataContainerOnBlobstore
    }

    // TODO(wchan): There has to be a better way to do this... contact Xyzmo about user management.
    val xyzmoUID: String = celebrity.getXyzmoUID()
    //    XyzmoBiometricServices.deleteUser(userId = xyzmoUID)
    TestXyzmoBiometricServices.addUser(userId = xyzmoUID, userName = celebrity.publicName.get)
    TestXyzmoBiometricServices.addProfile(userId = xyzmoUID, profileName = xyzmoUID)
    val signatureDataContainers = for (signatureSample <- signatureSamples) yield blobs.get(SignatureSample.getXmlUrl(signatureSample.id)).get.asString
    val xyzmoEnrollDynamicProfileResponse = TestXyzmoBiometricServices.enrollUser(userId = xyzmoUID, profileName = xyzmoUID, signatureDataContainers = signatureDataContainers)
    val isSuccessfulSignatureEnrollment = xyzmoEnrollDynamicProfileResponse.isSuccessfulSignatureEnrollment || xyzmoEnrollDynamicProfileResponse.isProfileAlreadyEnrolled

    println("Result of signature enrollment attempt for celebrity " + celebrity.id.toString + ": " + isSuccessfulSignatureEnrollment.toString)

    isSuccessfulSignatureEnrollment
  }

  private def sendStartEnrollmentRequest(celebrity: Celebrity): VBGRequest = {
    val startEnrollmentRequest = VBGDevRandomNumberBiometricServices.sendStartEnrollmentRequest(celebrity.id.toString, false)
    if (startEnrollmentRequest.getResponseValue(VBGRequest._errorCode) == "0") {
      // First-time enrollment
      startEnrollmentRequest
    }
    else {
      // Re-enrollment
      VBGDevRandomNumberBiometricServices.sendStartEnrollmentRequest(celebrity.id.toString, true)
    }
  }

  def attemptVoiceEnrollment(celebrity: Celebrity, voiceSamples: scala.List[VoiceSample]): Boolean = {
    val startEnrollmentRequest = sendStartEnrollmentRequest(celebrity)
    val transactionId = startEnrollmentRequest.getResponseValue(VBGRequest._transactionId)
    println("Attempting voice enrollment with transactionId " + transactionId)
    //    assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode))
    //    assertNotNull(transactionId)

    var atLeastOneUsableSample = false
    for (voiceSample <- voiceSamples) {
      val audioCheckRequest = VBGDevRandomNumberBiometricServices.sendAudioCheckRequest(transactionId, VoiceSample.getWavUrl(voiceSample.id))
      val errorCode = audioCheckRequest.getResponseValue(VBGRequest._errorCode)
      if (errorCode == "0") atLeastOneUsableSample = true
      val usableTime = audioCheckRequest.getResponseValue(VBGRequest._usableTime)
      println(errorCode + " " + usableTime)
      // store metadata on VoiceSample... ignoring errorcodes for now
    }
    if (!atLeastOneUsableSample) {
      println("No usable voice samples... aborting enrollment attempt!")
      return false
    }

    val enrollUserRequest = VBGDevRandomNumberBiometricServices.sendEnrollUserRequest(transactionId)
    val enrollmentSuccessValue = enrollUserRequest.getResponseValue(VBGRequest._success)

    VBGDevRandomNumberBiometricServices.sendFinishEnrollTransactionRequest(transactionId, enrollmentSuccessValue)

    val isSuccessfulVoiceEnrollment = enrollmentSuccessValue == "true"

    println("Result of voice enrollment attempt for celebrity " + celebrity.id.toString + ": " + isSuccessfulVoiceEnrollment.toString)

    isSuccessfulVoiceEnrollment
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
