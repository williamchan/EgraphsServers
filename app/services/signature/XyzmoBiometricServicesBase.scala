package services.signature

import com.xyzmo.wwww.biometricserver.{SDCFromJSONStub, WebServiceUserAndProfileStub, WebServiceBiometricPartStub}
import com.xyzmo.wwww.biometricserver.SDCFromJSONStub._
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub._
import models.xyzmo._
import org.apache.axis2.transport.http.{HTTPConstants, HttpTransportProperties}
import org.apache.log4j.Logger
import models.{EnrollmentSample, Egraph, EnrollmentBatch}

trait XyzmoBiometricServicesBase {

  protected val log: Logger
  protected val isBasicAuth: Boolean
  protected val username: String = "usermanager"
  protected val password: String = "%User%01"
  protected val domain: String
  protected val host: String
  protected val port: Int = 50200

  def enroll(enrollmentBatch: EnrollmentBatch): Either[SignatureBiometricsError, Boolean] = {
    val enrollmentSamples: List[EnrollmentSample] = enrollmentBatch.getEnrollmentSamples
    val signatureDataContainers: List[String] = for (enrollmentSample <- enrollmentSamples) yield getXyzmoSignatureDataContainer(enrollmentSample)

    val celebrityId: Long = enrollmentBatch.celebrityId
    val xyzmoDeleteUser: XyzmoDeleteUser = XyzmoBiometricServices.deleteUser(enrollmentBatchId = enrollmentBatch.id, userId = celebrityId.toString)
    xyzmoDeleteUser.save()
    val xyzmoAddUser: XyzmoAddUser = addUser(enrollmentBatchId = enrollmentBatch.id, userId = celebrityId.toString)
    xyzmoAddUser.save()
    val xyzmoAddProfile: XyzmoAddProfile = addProfile(enrollmentBatchId = enrollmentBatch.id, userId = celebrityId.toString)
    xyzmoAddProfile.save()
    val xyzmoEnrollDynamicProfile: XyzmoEnrollDynamicProfile = enrollUser(enrollmentBatchId = enrollmentBatch.id, userId = celebrityId.toString, signatureDataContainers = signatureDataContainers)
    xyzmoEnrollDynamicProfile.save()

    // todo(wchan): What situations should result in Left(SignatureBiometricsError)?
    val isSuccessfulSignatureEnrollment = xyzmoEnrollDynamicProfile.isSuccessfulSignatureEnrollment || xyzmoEnrollDynamicProfile.isProfileAlreadyEnrolled
    println("Result of signature enrollment attempt for celebrity " + celebrityId.toString + ": " + isSuccessfulSignatureEnrollment.toString)
    Right(isSuccessfulSignatureEnrollment)
  }

  def verify(signatureJson: String, egraph: Egraph): Either[SignatureBiometricsError, SignatureVerificationMetadata] = {
    val sdc = XyzmoBiometricServices.getSignatureDataContainerFromJSON(signatureJson)
    val xyzmoVerifyUser: XyzmoVerifyUser = XyzmoBiometricServices.verifyUser(egraphId = egraph.id, userId = egraph.celebrity.id.toString, sdc)
    xyzmoVerifyUser.save()

    Right(SignatureVerificationMetadata(
      success = xyzmoVerifyUser.isMatch.getOrElse(false),
      score = xyzmoVerifyUser.score
    ))
  }

  protected[signature] def addUser(enrollmentBatchId: Long, userId: String): XyzmoAddUser = {
    val user_Add: User_Add_v1 = new User_Add_v1
    user_Add.setBioUserId(userId)
    user_Add.setDisplayName(userId)
    user_Add.setBioUserStatus(BioUserStatus.Active)
    val webServiceUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val user_Add_v1Response: User_Add_v1Response = webServiceUserAndProfile.get.user_Add_v1(user_Add)
    val xyzmoAddUser: XyzmoAddUser = new XyzmoAddUser(enrollmentBatchId = enrollmentBatchId).withResultBase(user_Add_v1Response.getUser_Add_v1Result)
    if (xyzmoAddUser.baseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue) {
      log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      log.error("Error during User_Add_v1: " + xyzmoAddUser.errorMsg.getOrElse(None))
      //      if (error eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      //      }
    }
    xyzmoAddUser
  }

  protected[signature] def deleteUser(enrollmentBatchId: Long, userId: String): XyzmoDeleteUser = {
    val user_Delete: User_Delete_v1 = new User_Delete_v1
    user_Delete.setBioUserId(userId)
    val webServiceUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val user_Delete_v1Response: User_Delete_v1Response = webServiceUserAndProfile.get.user_Delete_v1(user_Delete)
    val xyzmoDeleteUser: XyzmoDeleteUser = new XyzmoDeleteUser(enrollmentBatchId = enrollmentBatchId).withResultBase(user_Delete_v1Response.getUser_Delete_v1Result)
    if (xyzmoDeleteUser.baseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue) {
      log.info("User_Delete_v1 succeeded: User " + userId + " has been deleted successfully.")
    }
    else {
      log.error("Error during User_Delete_v1: " + xyzmoDeleteUser.errorMsg.getOrElse(None))
    }
    xyzmoDeleteUser
  }

  protected[signature] def addProfile(enrollmentBatchId: Long, userId: String): XyzmoAddProfile = {
    val profile_Add: Profile_Add_v1 = new Profile_Add_v1
    profile_Add.setBioUserId(userId)
    profile_Add.setProfileName(userId)
    profile_Add.setProfileType(ProfileType.Dynamic)
    val webServiceUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val profile_Add_v1Response: Profile_Add_v1Response = webServiceUserAndProfile.get.profile_Add_v1(profile_Add)
    val xyzmoAddProfile: XyzmoAddProfile = new XyzmoAddProfile(enrollmentBatchId = enrollmentBatchId).withProfile_Add_v1Response(profile_Add_v1Response)
    if (xyzmoAddProfile.baseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue) {
      log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.")
    }
    else {
      log.error("Error during Profile_Add_v1: " + xyzmoAddProfile.errorMsg.getOrElse(None))
    }
    xyzmoAddProfile
  }

  // TODO(wchan): What to do if preceding call to addProfile is done for profileName that already exists?
  protected[signature] def enrollUser(enrollmentBatchId: Long, userId: String, signatureDataContainers: List[String]): XyzmoEnrollDynamicProfile = {
    val SignatureDataContainerXmlStrArr: WebServiceBiometricPartStub.ArrayOfString = new WebServiceBiometricPartStub.ArrayOfString
    for (sdc <- signatureDataContainers) SignatureDataContainerXmlStrArr.addString(sdc)
    val enrollDynamicProfile: EnrollDynamicProfile_v1 = new EnrollDynamicProfile_v1
    enrollDynamicProfile.setBioUserId(userId)
    enrollDynamicProfile.setProfileName(userId)
    enrollDynamicProfile.setContinuous(false)
    enrollDynamicProfile.setSignatureDataContainerXmlStrArr(SignatureDataContainerXmlStrArr)
    val webServiceBiometricPart: Option[WebServiceBiometricPartStub] = getWebServiceBiometricPartStub
    val enrollDynamicProfile_v1Response: EnrollDynamicProfile_v1Response = webServiceBiometricPart.get.enrollDynamicProfile_v1(enrollDynamicProfile)
    val xyzmoEnrollDynamicProfile: XyzmoEnrollDynamicProfile = new XyzmoEnrollDynamicProfile(enrollmentBatchId = enrollmentBatchId).withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response)
    if (xyzmoEnrollDynamicProfile.baseResult eq WebServiceBiometricPartStub.BaseResultEnum.ok.getValue) {
      val enrollResult = xyzmoEnrollDynamicProfile.enrollResult
      log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult.getOrElse(None))
      enrollResult.getOrElse(None) match {
        case "EnrollCompleted" => {
          log.info("EnrollDynamicProfile_v1: Profile " + xyzmoEnrollDynamicProfile.xyzmoProfileId.getOrElse(None) + " created; contains " + xyzmoEnrollDynamicProfile.nrEnrolled.getOrElse(None) + " signatures.")
        }
        case "EnrollRejected" => {
          log.info(xyzmoEnrollDynamicProfile.rejectedSignaturesSummary.getOrElse(None))
        }
        case "EnrollContinued:" => {
          // This should never happen.
        }
      }
    }
    else {
      log.error("Error during EnrollDynamicProfile_v1: " + xyzmoEnrollDynamicProfile.errorMsg.getOrElse(None))
    }
    xyzmoEnrollDynamicProfile
  }

  protected[signature] def verifyUser(egraphId: Long, userId: String, signatureDCToVerify: String): XyzmoVerifyUser = {
    val verifyUserBySignatureDynamicToDynamic_v1: VerifyUserBySignatureDynamicToDynamic_v1 = new VerifyUserBySignatureDynamicToDynamic_v1
    verifyUserBySignatureDynamicToDynamic_v1.setBioUserId(userId)
    verifyUserBySignatureDynamicToDynamic_v1.setSignatureDataContainerXmlStr(signatureDCToVerify)
    val webServiceBiometricPart: Option[WebServiceBiometricPartStub] = getWebServiceBiometricPartStub
    val verifyUserBySignatureDynamicToDynamic_v1Response: VerifyUserBySignatureDynamicToDynamic_v1Response = webServiceBiometricPart.get.verifyUserBySignatureDynamicToDynamic_v1(verifyUserBySignatureDynamicToDynamic_v1)
    val xyzmoVerifyUser: XyzmoVerifyUser = new XyzmoVerifyUser(egraphId = egraphId).withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response)
    xyzmoVerifyUser
  }

  protected[signature] def getSignatureDataContainerFromJSON(jsonStr: String): String = {
    val json: GetSignatureDataContainerFromJSON = new GetSignatureDataContainerFromJSON()
    json.setJsonData(jsonStr)
    val sdcFromJSON: Option[SDCFromJSONStub] = getSDCFromJSON
    val getSignatureDataContainerFromJSONResponse: GetSignatureDataContainerFromJSONResponse = sdcFromJSON.get.getSignatureDataContainerFromJSON(json)
    getSignatureDataContainerFromJSONResponse.getGetSignatureDataContainerFromJSONResult
  }

  private def getXyzmoSignatureDataContainer(enrollmentSample: EnrollmentSample): String = {
    val jsonStr: String = enrollmentSample.getSignatureJson
    val sdc = getSignatureDataContainerFromJSON(jsonStr)
    enrollmentSample.putSignatureXml(sdc)
    sdc
  }

  // ====================================== Services

  private def getWebServiceBiometricPartStub: Option[WebServiceBiometricPartStub] = {
    try {
      val webServiceBiometricPart = new WebServiceBiometricPartStub(getWebServiceBiometricPartUrl)
      val authenticator: HttpTransportProperties.Authenticator = getAuthenticator
      webServiceBiometricPart._getServiceClient.getOptions.setProperty(HTTPConstants.AUTHENTICATE, authenticator)
      Some(webServiceBiometricPart)
    }
    catch {
      case e: Exception => {
        log.error("Trouble initializing WebServiceBiometricPartStub.", e)
        None
      }
    }
  }

  private def getWebServiceUserAndProfileStub: Option[WebServiceUserAndProfileStub] = {
    try {
      val webServiceUserAndProfile = new WebServiceUserAndProfileStub(getWebServiceUserAndProfileUrl)
      val authenticator: HttpTransportProperties.Authenticator = getAuthenticator
      webServiceUserAndProfile._getServiceClient.getOptions.setProperty(HTTPConstants.AUTHENTICATE, authenticator)
      Some(webServiceUserAndProfile)
    }
    catch {
      case e: Exception => {
        log.error("Trouble initializing WebServiceUserAndProfileStub.", e)
        None
      }
    }
  }

  private def getSDCFromJSON: Option[SDCFromJSONStub] = {
    try {
      Some(new SDCFromJSONStub)
    }
    catch {
      case e: Exception => {
        log.error("Trouble initializing SDCFromJSONStub.", e)
        None
      }
    }
  }

  private def getWebServiceBiometricPartUrl: String = {
    "http://" + host + ":" + port + "/WebServices/WebServiceBiometricPart.asmx"
  }

  private def getWebServiceUserAndProfileUrl: String = {
    "http://" + host + ":" + port + "/WebServices/WebServiceUserAndProfile.asmx"
  }

  private def getAuthenticator: HttpTransportProperties.Authenticator = {
    val authenticator: HttpTransportProperties.Authenticator = new HttpTransportProperties.Authenticator
    authenticator.setHost(host)
    authenticator.setPort(port)
    authenticator.setDomain(domain)
    authenticator.setUsername(username)
    authenticator.setPassword(password)
    authenticator.setPreemptiveAuthentication(true)
    if (isBasicAuth) {
      val authSchemes = new java.util.ArrayList[String]()
      authSchemes.add(HttpTransportProperties.Authenticator.BASIC)
      authenticator.setAuthSchemes(authSchemes)
    }
    authenticator
  }

}