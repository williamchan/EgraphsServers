package jobs

import db.Schema
import org.squeryl.PrimitiveTypeMode._
import play.jobs._
import models.{Celebrity, VoiceSample, SignatureSample, EnrollmentBatch}
import libs.Blobs
import Blobs.Conversions._
import services.signature.BiometricServerServices
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.{ProfileType, ErrorStatus, BaseResultEnum, BioUserStatus}
import com.xyzmo.wwww.biometricserver.{WebServiceBiometricPartStub, WebServiceUserAndProfileStub, SDCFromJSONStub}

@On("0 * * * * ?") // cron expression: seconds minutes hours day-of-month month day-of-week (year optional)
class EnrollmentBatchJob extends Job {

  //  Setup process to query for EnrollmentBatches that are {isBatchComplete = true and isSuccessfullEnrollment = null},
  //  and attempt enrollment. Failed enrollment changes Celebrity.enrollmentStatus to "NotEnrolled",
  //  whereas successful enrollment changes Celebrity.enrollmentStatus to "Enrolled".
  //
  //  1. Query for EnrollmentBatches for which isBatchComplete==true and isSuccessfullEnrollment.isNull
  //  2. For each, find all SignatureSamples and VoiceSamples via EnrollmentSamples
  //  3. For SignatureSamples, translate each to SignatureDataContainer and store that on BlobStore… then call enroll method with all SignatureSamples
  //  4. For VoiceSamples, call enroll method
  //  5. Update EnrollmentBatch and Celebrity


  override def doJob() {



    //  1. Query for EnrollmentBatches for which isBatchComplete==true and isSuccessfullEnrollment.isNull
    for (batch <- EnrollmentBatchJob.findEnrollmentBatchesPending()) {
      val celebrity = Celebrity.findById(batch.celebrityId).get

      //  2. For each batch, find all SignatureSamples and VoiceSamples via EnrollmentSamples
      val signatureSamples: List[SignatureSample] = EnrollmentBatchJob.getSignatureSamples(batch)
      val voiceSamples: List[VoiceSample] = EnrollmentBatchJob.getVoiceSamples(batch)
      var isSuccessfulEnrollment = true

      //  3. For SignatureSamples, translate each to SignatureDataContainer and store that on BlobStore… then call enroll method with all SignatureSamples
      for (signatureSample <- signatureSamples) {
        val sdc = getXyzmoSignatureDataContainer(signatureSample)
        Blobs.put(SignatureSample.getXmlUrl(signatureSample.id), sdc.getBytes)
      }
      addUser(userId = celebrity.id.toString, userName = celebrity.publicName.get)
      addProfile(userId = celebrity.id.toString, profileName = celebrity.id.toString)
      val signatureDataContainers = for (signatureSample <- signatureSamples) yield Blobs.get(SignatureSample.getXmlUrl(signatureSample.id)).get.asString
      enrollUser(userId = celebrity.id.toString, profileName = celebrity.id.toString, signatureDataContainers = signatureDataContainers)

      //  4. For VoiceSamples, call enroll method
      for (voiceSample <- voiceSamples) {
        voiceSample

      }

      //  5. Update EnrollmentBatch and Celebrity
      batch.copy(isSuccessfullEnrollment = Some(isSuccessfulEnrollment)).save()
      if (isSuccessfulEnrollment) {
        celebrity.copy(enrollmentStatus = models.Enrolled.value).save()
      }

    }
    Unit
  }

  def getXyzmoSignatureDataContainer(signatureSample: SignatureSample): String = {
    val json: SDCFromJSONStub.GetSignatureDataContainerFromJSON = new SDCFromJSONStub.GetSignatureDataContainerFromJSON
    val jsonStr: String = Blobs.get(SignatureSample.getJsonUrl(signatureSample.id)).get.asString
    json.setJsonData(jsonStr)
    val response: SDCFromJSONStub.GetSignatureDataContainerFromJSONResponse = EnrollmentBatchJob._webServiceSDCFromJSON.getSignatureDataContainerFromJSON(json)
    response.getGetSignatureDataContainerFromJSONResult
  }

  def addUser(userId: String, userName: String) {
    val user: WebServiceUserAndProfileStub.User_Add_v1 = new WebServiceUserAndProfileStub.User_Add_v1
    user.setBioUserId(userId)
    user.setDisplayName(userName)
    user.setBioUserStatus(BioUserStatus.Active)
    val userAddResponse: WebServiceUserAndProfileStub.User_Add_v1Response = EnrollmentBatchJob._services.getWebServiceProxyUserAndProfile.user_Add_v1(user)
    val user_add_v1Result: WebServiceUserAndProfileStub.ResultBase = userAddResponse.getUser_Add_v1Result
    if (user_add_v1Result.getBaseResult eq BaseResultEnum.ok) {
      BiometricServerServices.log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      BiometricServerServices.log.error("Error during User_Add_v1: " + user_add_v1Result.getErrorInfo.getErrorMsg)
      if (user_add_v1Result.getErrorInfo.getError eq ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  def addProfile(userId: String, profileName: String) {
    val profile: WebServiceUserAndProfileStub.Profile_Add_v1 = new WebServiceUserAndProfileStub.Profile_Add_v1
    profile.setBioUserId(userId)
    profile.setProfileName(profileName)
    profile.setProfileType(ProfileType.Dynamic)
    val profileAddResponse: WebServiceUserAndProfileStub.Profile_Add_v1Response = EnrollmentBatchJob._services.getWebServiceProxyUserAndProfile.profile_Add_v1(profile)
    val profile_add_v1Result: WebServiceUserAndProfileStub.ProfileInfoResult_v1 = profileAddResponse.getProfile_Add_v1Result
    if (profile_add_v1Result.getBaseResult eq BaseResultEnum.ok) {
      BiometricServerServices.log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.")
    }
    else {
      BiometricServerServices.log.error("Error during Profile_Add_v1: " + profile_add_v1Result.getErrorInfo.getErrorMsg)
      if (profile_add_v1Result.getErrorInfo.getError eq ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  def enrollUser(userId: String, profileName: String, signatureDataContainers: List[String]) {
    val signatures: WebServiceBiometricPartStub.ArrayOfString = new WebServiceBiometricPartStub.ArrayOfString
    for (sdc <- signatureDataContainers) {
      signatures.addString(sdc)
    }
    val enrollProfileInfo: WebServiceBiometricPartStub.EnrollDynamicProfile_v1 = new WebServiceBiometricPartStub.EnrollDynamicProfile_v1
    enrollProfileInfo.setBioUserId(userId)
    enrollProfileInfo.setProfileName(profileName)
    enrollProfileInfo.setContinuous(false)
    enrollProfileInfo.setSignatureDataContainerXmlStrArr(signatures)
    val enrollDynamicResponse1: WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response = EnrollmentBatchJob._services.getWebServiceProxyBiometricPart.enrollDynamicProfile_v1(enrollProfileInfo)
    val enrollResult1: WebServiceBiometricPartStub.EnrollResultInfo_v1 = enrollDynamicResponse1.getEnrollDynamicProfile_v1Result
    if (enrollResult1.getBaseResult eq WebServiceBiometricPartStub.BaseResultEnum.ok) {
      BiometricServerServices.log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult1.getOkInfo.getEnrollResult.getValue)
      BiometricServerServices.log.info("EnrollDynamicProfile_v1: Profile " + enrollResult1.getOkInfo.getInfoEnrollOk.getProfileId + " created; contains " + enrollResult1.getOkInfo.getInfoEnrollOk.getNrEnrolled + " signatures.")
    }
    else {
      BiometricServerServices.log.error("Error during EnrollDynamicProfile_v1: " + enrollResult1.getErrorInfo.getErrorMsg)
      if (enrollResult1.getErrorInfo.getError eq WebServiceBiometricPartStub.ErrorStatus.ProfileAlreadyEnrolled) {
      }
    }
  }

}

object EnrollmentBatchJob {

  val _webServiceSDCFromJSON: SDCFromJSONStub = new SDCFromJSONStub
  val _services: BiometricServerServices = new BiometricServerServices

  def findEnrollmentBatchesPending(): List[EnrollmentBatch] = {
    from(Schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.isBatchComplete === true and enrollmentBatch.isSuccessfullEnrollment.isNull)
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