package services.signature

import com.xyzmo.wwww.biometricserver.{SDCFromJSONStub, WebServiceUserAndProfileStub, WebServiceBiometricPartStub}
import com.xyzmo.wwww.biometricserver.SDCFromJSONStub._
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub._
import java.util.ArrayList
import org.apache.axis2.transport.http.{HTTPConstants, HttpTransportProperties}
import org.apache.log4j.Logger

trait SignatureBiometricServicesTrait {

  protected val log: Logger
  protected val isBasicAuth: Boolean
  protected val username: String = "usermanager"
  protected val password: String = "%User%01"
  protected val domain: String
  protected val host: String
  protected val port: Int = 50200

  def addUser(userId: String, userName: String): (String, String, String) = {
    val user_Add: User_Add_v1 = new User_Add_v1
    user_Add.setBioUserId(userId)
    user_Add.setDisplayName(userName)
    user_Add.setBioUserStatus(BioUserStatus.Active)
    val webServiceProxyUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val userAddResponse: User_Add_v1Response = webServiceProxyUserAndProfile.get.user_Add_v1(user_Add)
    val user_add_v1Result: WebServiceUserAndProfileStub.ResultBase = userAddResponse.getUser_Add_v1Result
    val baseResult: WebServiceUserAndProfileStub.BaseResultEnum = user_add_v1Result.getBaseResult
    // todo getErrorInfo can be null

    val error: WebServiceUserAndProfileStub.ErrorStatus = user_add_v1Result.getErrorInfo.getError
    val errorMsg: String = user_add_v1Result.getErrorInfo.getErrorMsg
    if (baseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      log.error("Error during User_Add_v1: " + errorMsg)
      //      if (error eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      //      }
    }
    (baseResult.getValue, error.getValue, errorMsg)
  }

  def deleteUser(userId: String): WebServiceUserAndProfileStub.ResultBase = {
    val user_Delete: User_Delete_v1 = new User_Delete_v1
    user_Delete.setBioUserId(userId)
    val webServiceProxyUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val userDeleteResponse: User_Delete_v1Response = webServiceProxyUserAndProfile.get.user_Delete_v1(user_Delete)
    val user_delete_v1Result: WebServiceUserAndProfileStub.ResultBase = userDeleteResponse.getUser_Delete_v1Result
    val baseResult: WebServiceUserAndProfileStub.BaseResultEnum = user_delete_v1Result.getBaseResult
    // todo getErrorInfo can be null
    val error: WebServiceUserAndProfileStub.ErrorStatus = user_delete_v1Result.getErrorInfo.getError
    val errorMsg: String = user_delete_v1Result.getErrorInfo.getErrorMsg
    if (baseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      log.info("User_Delete_v1 succeeded: User " + userId + " has been deleted successfully.")
    }
    else {
      log.error("Error during User_Delete_v1: " + user_delete_v1Result.getErrorInfo.getErrorMsg)
      if (user_delete_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
    println("User_Delete_v1: " + user_delete_v1Result.getBaseResult.getValue + "... " + user_delete_v1Result.getErrorInfo.getErrorMsg)
    user_delete_v1Result
  }

  def addProfile(userId: String, profileName: String) {
    val profile_Add: Profile_Add_v1 = new Profile_Add_v1
    profile_Add.setBioUserId(userId)
    profile_Add.setProfileName(profileName)
    profile_Add.setProfileType(ProfileType.Dynamic)
    val webServiceProxyUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val profileAddResponse: Profile_Add_v1Response = webServiceProxyUserAndProfile.get.profile_Add_v1(profile_Add)
    val profile_add_v1Result: ProfileInfoResult_v1 = profileAddResponse.getProfile_Add_v1Result
    if (profile_add_v1Result.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.")
    }
    else {
      log.error("Error during Profile_Add_v1: " + profile_add_v1Result.getErrorInfo.getErrorMsg)
      if (profile_add_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  def javatest_EnrollUser(userId: String, profileName: String, signature1: String, signature2: String, signature3: String, signature4: String, signature5: String, signature6: String) {
    enrollUser(userId, profileName, List[String](signature1, signature2, signature3, signature4, signature5, signature6))
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
    val webServiceProxyBiometricPart: Option[WebServiceBiometricPartStub] = getWebServiceBiometricPartStub
    val enrollDynamicResponse1: EnrollDynamicProfile_v1Response = webServiceProxyBiometricPart.get.enrollDynamicProfile_v1(enrollDynamicProfile)
    val enrollResult1: EnrollResultInfo_v1 = enrollDynamicResponse1.getEnrollDynamicProfile_v1Result
    if (enrollResult1.getBaseResult eq WebServiceBiometricPartStub.BaseResultEnum.ok) {
      val enrollResult = enrollResult1.getOkInfo.getEnrollResult.getValue
      log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult)
      enrollResult match {
        case "EnrollCompleted" => {
          log.info("EnrollDynamicProfile_v1: Profile " + enrollResult1.getOkInfo.getInfoEnrollOk.getProfileId + " created; contains " + enrollResult1.getOkInfo.getInfoEnrollOk.getNrEnrolled + " signatures.")
          true
        }
        case "EnrollRejected" => {
          if (enrollResult1.getOkInfo.getRejectedSignatures != null) {
            val rejectedSignatures: Array[RejectedSignature] = enrollResult1.getOkInfo.getRejectedSignatures.getRejectedSignature
            log.info("EnrollDynamicProfile_v1: " + rejectedSignatures.length + " signatures rejected. Reasons follow:")
            for (rejectedSignature <- rejectedSignatures) {
              log.info("Rejection reason: " + rejectedSignature.getReason.toString)
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
      log.error("Error during EnrollDynamicProfile_v1: " + enrollResult1.getErrorInfo.getErrorMsg)
      if (enrollResult1.getErrorInfo.getError eq WebServiceBiometricPartStub.ErrorStatus.ProfileAlreadyEnrolled) {
      }
      false
    }
  }

  def verifyUser(userId: String, signatureDCToVerify: String): (String, Boolean, Int) = {
    val verifyUserBySignatureDynamicToDynamic_v1: VerifyUserBySignatureDynamicToDynamic_v1 = new VerifyUserBySignatureDynamicToDynamic_v1
    verifyUserBySignatureDynamicToDynamic_v1.setBioUserId(userId)
    verifyUserBySignatureDynamicToDynamic_v1.setSignatureDataContainerXmlStr(signatureDCToVerify)
    val webServiceProxyBiometricPart: Option[WebServiceBiometricPartStub] = getWebServiceBiometricPartStub
    val verifyResponse: VerifyUserBySignatureDynamicToDynamic_v1Response = webServiceProxyBiometricPart.get.verifyUserBySignatureDynamicToDynamic_v1(verifyUserBySignatureDynamicToDynamic_v1)
    val verifyResult: VerifyResultInfo_v1 = verifyResponse.getVerifyUserBySignatureDynamicToDynamic_v1Result

    val baseResult: WebServiceBiometricPartStub.BaseResultEnum = verifyResult.getBaseResult
    val okInfo: VerifyResult = verifyResult.getOkInfo
    //    okInfo.getScore
    //    okInfo.getVerifyResult
    //    okInfo.getQualityLevel
    //    val errorInfo: WebServiceBiometricPartStub.ErrorInfo = verifyResult.getErrorInfo
    //    errorInfo.getError
    //    errorInfo.getErrorMsg
    // todo encapsulate into an object?

    if (okInfo != null) (baseResult.getValue, okInfo.getVerifyResult == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch, okInfo.getScore)
    else (baseResult.getValue, false, 0)

  }

  def getSignatureDataContainerFromJSON(jsonStr: String): String = {
    val json: GetSignatureDataContainerFromJSON = new GetSignatureDataContainerFromJSON()
    json.setJsonData(jsonStr)
    val sdcFromJSON: Option[SDCFromJSONStub] = getSDCFromJSON
    val response: GetSignatureDataContainerFromJSONResponse = sdcFromJSON.get.getSignatureDataContainerFromJSON(json)
    response.getGetSignatureDataContainerFromJSONResult
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
      val authSchemes = new ArrayList[String]()
      authSchemes.add(HttpTransportProperties.Authenticator.BASIC)
      authenticator.setAuthSchemes(authSchemes)
    }
    authenticator
  }

}
