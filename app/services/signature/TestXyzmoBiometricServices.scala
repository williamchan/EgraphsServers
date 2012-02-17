package services.signature

import org.apache.axis2.transport.http.{HTTPConstants, HttpTransportProperties}
import org.apache.log4j.Logger
import test.xyzmo.wwww.biometricserver.{SDCFromJSONStub, WebServiceUserAndProfileStub, WebServiceBiometricPartStub}
import test.xyzmo.wwww.biometricserver.SDCFromJSONStub._
import test.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._
import test.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub._

object TestXyzmoBiometricServices {

  private val log: Logger = Logger.getLogger(TestXyzmoBiometricServices.getClass)
  private val username: String = "usermanager"
  private val password: String = "%User%01"
  private val domain: String = "testlab"
  private val port: Int = 50200
  private val host: String = "testlab.xyzmo.com"
  private val webServiceBiometricPartUrl: String = "http://" + host + ":" + port + "/WebServices/WebServiceBiometricPart.asmx"
  private val webServiceUserAndProfileUrl: String = "http://" + host + ":" + port + "/WebServices/WebServiceUserAndProfile.asmx"

  def getSignatureDataContainerFromJSON(jsonStr: String): GetSignatureDataContainerFromJSONResponse = {
    // TODO: remove this patch once we have final endpoint to translate JSON signatures to Xyzmo's data format.
    val patchedJsonStr = jsonStr.replace("x", "originalX").replace("y", "originalY").replace("t", "time")
    val json: GetSignatureDataContainerFromJSON = new GetSignatureDataContainerFromJSON()
    json.setJsonData(patchedJsonStr)
    val response: GetSignatureDataContainerFromJSONResponse = getSDCFromJSON.getSignatureDataContainerFromJSON(json)
    response
  }

  def deleteUser(userId: String) {
    val user_Delete: User_Delete_v1 = new User_Delete_v1
    user_Delete.setBioUserId(userId)
    val userDeleteResponse: User_Delete_v1Response = getWebServiceProxyUserAndProfile.get.user_Delete_v1(user_Delete)
    val user_delete_v1Result: WebServiceUserAndProfileStub.ResultBase = userDeleteResponse.getUser_Delete_v1Result
    if (user_delete_v1Result.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      TestXyzmoBiometricServices.log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      TestXyzmoBiometricServices.log.error("Error during User_Add_v1: " + user_delete_v1Result.getErrorInfo.getErrorMsg)
      if (user_delete_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
    println("User_Delete_v1: " + user_delete_v1Result.getBaseResult.getValue + "... " + user_delete_v1Result.getErrorInfo.getErrorMsg)
  }

  def addUser(userId: String, userName: String) {
    val user_Add: User_Add_v1 = new User_Add_v1
    user_Add.setBioUserId(userId)
    user_Add.setDisplayName(userName)
    user_Add.setBioUserStatus(BioUserStatus.Active)
    val profile: Option[WebServiceUserAndProfileStub] = getWebServiceProxyUserAndProfile
    val userAddResponse: User_Add_v1Response = profile.get.user_Add_v1(user_Add)
    val user_add_v1Result: WebServiceUserAndProfileStub.ResultBase = userAddResponse.getUser_Add_v1Result
    if (user_add_v1Result.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      TestXyzmoBiometricServices.log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      TestXyzmoBiometricServices.log.error("Error during User_Add_v1: " + user_add_v1Result.getErrorInfo.getErrorMsg)
      if (user_add_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  def addProfile(userId: String, profileName: String) {
    val profile_Add: Profile_Add_v1 = new Profile_Add_v1
    profile_Add.setBioUserId(userId)
    profile_Add.setProfileName(profileName)
    profile_Add.setProfileType(ProfileType.Dynamic)
    val profileAddResponse: Profile_Add_v1Response = getWebServiceProxyUserAndProfile.get.profile_Add_v1(profile_Add)
    val profile_add_v1Result: ProfileInfoResult_v1 = profileAddResponse.getProfile_Add_v1Result
    if (profile_add_v1Result.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      TestXyzmoBiometricServices.log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.")
    }
    else {
      TestXyzmoBiometricServices.log.error("Error during Profile_Add_v1: " + profile_add_v1Result.getErrorInfo.getErrorMsg)
      if (profile_add_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  // TODO(wchan): What to do if preceding call to addProfile is done for profileName that already exists?
  def enrollUser(userId: String, profileName: String, signatureDataContainers: List[String]): Boolean = {
    val SignatureDataContainerXmlStrArr: WebServiceBiometricPartStub.ArrayOfString = new WebServiceBiometricPartStub.ArrayOfString
    for (sdc <- signatureDataContainers) SignatureDataContainerXmlStrArr.addString(sdc)
    val enrollDynamicProfile: EnrollDynamicProfile_v1 = new EnrollDynamicProfile_v1
    enrollDynamicProfile.setBioUserId(userId)
    enrollDynamicProfile.setProfileName(profileName)
    enrollDynamicProfile.setContinuous(false)
    enrollDynamicProfile.setSignatureDataContainerXmlStrArr(SignatureDataContainerXmlStrArr)
    val enrollDynamicResponse1: EnrollDynamicProfile_v1Response = getWebServiceProxyBiometricPart.get.enrollDynamicProfile_v1(enrollDynamicProfile)
    val enrollResult1: EnrollResultInfo_v1 = enrollDynamicResponse1.getEnrollDynamicProfile_v1Result
    if (enrollResult1.getBaseResult eq WebServiceBiometricPartStub.BaseResultEnum.ok) {
      val enrollResult = enrollResult1.getOkInfo.getEnrollResult.getValue
      TestXyzmoBiometricServices.log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult)
      enrollResult match {
        case "EnrollCompleted" => {
          TestXyzmoBiometricServices.log.info("EnrollDynamicProfile_v1: Profile " + enrollResult1.getOkInfo.getInfoEnrollOk.getProfileId + " created; contains " + enrollResult1.getOkInfo.getInfoEnrollOk.getNrEnrolled + " signatures.")
          true
        }
        case "EnrollRejected" => {
          if (enrollResult1.getOkInfo.getRejectedSignatures != null) {
            val rejectedSignatures: Array[RejectedSignature] = enrollResult1.getOkInfo.getRejectedSignatures.getRejectedSignature
            TestXyzmoBiometricServices.log.info("EnrollDynamicProfile_v1: " + rejectedSignatures.length + " signatures rejected. Reasons follow:")
            for (rejectedSignature <- rejectedSignatures) {
              TestXyzmoBiometricServices.log.info("Rejection reason: " + rejectedSignature.getReason.toString)
              // TODO(wchan): Ask Xyzmo for rejectable signatures. Are they in the same order as signatureDataContainers?
              // Store this information.
            }
          }
          false
        }
        case "EnrollContinued:" => {
          // todo(wchan): Ask Xyzmo what continuous enrollment is for. We probably don't want to use this.
          false
        }
      }
    }
    else {
      TestXyzmoBiometricServices.log.error("Error during EnrollDynamicProfile_v1: " + enrollResult1.getErrorInfo.getErrorMsg)
      if (enrollResult1.getErrorInfo.getError eq WebServiceBiometricPartStub.ErrorStatus.ProfileAlreadyEnrolled) {
      }
      false
    }
  }

  def verifyUser(userId: String, signatureDCToVerify: String): VerifyResultInfo_v1 = {
    val verifyUserBySignatureDynamicToDynamic_v1: VerifyUserBySignatureDynamicToDynamic_v1 = new VerifyUserBySignatureDynamicToDynamic_v1
    verifyUserBySignatureDynamicToDynamic_v1.setBioUserId(userId)
    verifyUserBySignatureDynamicToDynamic_v1.setSignatureDataContainerXmlStr(signatureDCToVerify)
    val verifyResponse: VerifyUserBySignatureDynamicToDynamic_v1Response = getWebServiceProxyBiometricPart.get.verifyUserBySignatureDynamicToDynamic_v1(verifyUserBySignatureDynamicToDynamic_v1)
    val verifyResult: VerifyResultInfo_v1 = verifyResponse.getVerifyUserBySignatureDynamicToDynamic_v1Result
    verifyResult
  }

  // ====================================== Services

  private def getWebServiceProxyBiometricPart: Option[WebServiceBiometricPartStub] = {
    try {
      val webServiceBiometricPart = new WebServiceBiometricPartStub(webServiceBiometricPartUrl)
      val authenticator: HttpTransportProperties.Authenticator = getAuthenticator
      webServiceBiometricPart._getServiceClient.getOptions.setProperty(HTTPConstants.AUTHENTICATE, authenticator)
      Some(webServiceBiometricPart)
    }
    catch {
      case e: Exception => {
        log.error("Troubles initializing WebService BiometricPart.", e)
        None
      }
    }
  }

  private def getWebServiceProxyUserAndProfile: Option[WebServiceUserAndProfileStub] = {
    try {
      val webServiceUserAndProfile = new WebServiceUserAndProfileStub(webServiceUserAndProfileUrl)
      val authenticator: HttpTransportProperties.Authenticator = getAuthenticator
      webServiceUserAndProfile._getServiceClient.getOptions.setProperty(HTTPConstants.AUTHENTICATE, authenticator)
      Some(webServiceUserAndProfile)
    }
    catch {
      case e: Exception => {
        log.error("Troubles initializing WebService UserAndProfile", e)
        None
      }
    }
  }

  private def getSDCFromJSON: SDCFromJSONStub = {
    new SDCFromJSONStub
  }

  private def getAuthenticator: HttpTransportProperties.Authenticator = {
    val authenticator: HttpTransportProperties.Authenticator = new HttpTransportProperties.Authenticator
    authenticator.setHost(host)
    authenticator.setPort(port)
    authenticator.setDomain(domain)
    authenticator.setUsername(username)
    authenticator.setPassword(password)
    authenticator.setPreemptiveAuthentication(true)
    authenticator
  }
}