package services.signature

import com.xyzmo.wwww.biometricserver.{SDCFromJSONStub, WebServiceUserAndProfileStub, WebServiceBiometricPartStub}
import com.xyzmo.wwww.biometricserver.SDCFromJSONStub._
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub._
import org.apache.axis2.transport.http.{HTTPConstants, HttpTransportProperties}
import org.apache.log4j.Logger

trait XyzmoBiometricServicesBase {

  protected val log: Logger
  protected val isBasicAuth: Boolean
  protected val username: String = "usermanager"
  protected val password: String = "%User%01"
  protected val domain: String
  protected val host: String
  protected val port: Int = 50200

  def addUser(userId: String, userName: String): XyzmoAddUserResponse = {
    val user_Add: User_Add_v1 = new User_Add_v1
    user_Add.setBioUserId(userId)
    user_Add.setDisplayName(userName)
    user_Add.setBioUserStatus(BioUserStatus.Active)
    val webServiceUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val user_Add_v1Response: User_Add_v1Response = webServiceUserAndProfile.get.user_Add_v1(user_Add)
    val xyzmoAddUserResponse: XyzmoAddUserResponse = new XyzmoAddUserResponse(user_Add_v1Response)
    if (xyzmoAddUserResponse.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue) {
      log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      log.error("Error during User_Add_v1: " + xyzmoAddUserResponse.getErrorMsg.getOrElse(None))
      //      if (error eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      //      }
    }
    xyzmoAddUserResponse
  }

  def deleteUser(userId: String): XyzmoDeleteUserResponse = {
    val user_Delete: User_Delete_v1 = new User_Delete_v1
    user_Delete.setBioUserId(userId)
    val webServiceUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val user_Delete_v1Response: User_Delete_v1Response = webServiceUserAndProfile.get.user_Delete_v1(user_Delete)
    val xyzmoDeleteUserResponse: XyzmoDeleteUserResponse = new XyzmoDeleteUserResponse(user_Delete_v1Response)
    if (xyzmoDeleteUserResponse.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue) {
      log.info("User_Delete_v1 succeeded: User " + userId + " has been deleted successfully.")
    }
    else {
      log.error("Error during User_Delete_v1: " + xyzmoDeleteUserResponse.getErrorMsg.getOrElse(None))
    }
    xyzmoDeleteUserResponse
  }

  def addProfile(userId: String, profileName: String): XyzmoAddProfileResponse = {
    val profile_Add: Profile_Add_v1 = new Profile_Add_v1
    profile_Add.setBioUserId(userId)
    profile_Add.setProfileName(profileName)
    profile_Add.setProfileType(ProfileType.Dynamic)
    val webServiceUserAndProfile: Option[WebServiceUserAndProfileStub] = getWebServiceUserAndProfileStub
    val profile_Add_v1Response: Profile_Add_v1Response = webServiceUserAndProfile.get.profile_Add_v1(profile_Add)
    val xyzmoAddProfileResponse: XyzmoAddProfileResponse = new XyzmoAddProfileResponse(profile_Add_v1Response)
    if (xyzmoAddProfileResponse.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue) {
      log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.")
    }
    else {
      log.error("Error during Profile_Add_v1: " + xyzmoAddProfileResponse.getErrorMsg.getOrElse(None))
    }
    xyzmoAddProfileResponse
  }

  // TODO(wchan): What to do if preceding call to addProfile is done for profileName that already exists?
  def enrollUser(userId: String, profileName: String, signatureDataContainers: List[String]): XyzmoEnrollDynamicProfileResponse = {
    val SignatureDataContainerXmlStrArr: WebServiceBiometricPartStub.ArrayOfString = new WebServiceBiometricPartStub.ArrayOfString
    for (sdc <- signatureDataContainers) SignatureDataContainerXmlStrArr.addString(sdc)
    val enrollDynamicProfile: EnrollDynamicProfile_v1 = new EnrollDynamicProfile_v1
    enrollDynamicProfile.setBioUserId(userId)
    enrollDynamicProfile.setProfileName(profileName)
    enrollDynamicProfile.setContinuous(false)
    enrollDynamicProfile.setSignatureDataContainerXmlStrArr(SignatureDataContainerXmlStrArr)
    val webServiceBiometricPart: Option[WebServiceBiometricPartStub] = getWebServiceBiometricPartStub
    val enrollDynamicProfile_v1Response: EnrollDynamicProfile_v1Response = webServiceBiometricPart.get.enrollDynamicProfile_v1(enrollDynamicProfile)
    val xyzmoEnrollDynamicProfileResponse: XyzmoEnrollDynamicProfileResponse = new XyzmoEnrollDynamicProfileResponse(enrollDynamicProfile_v1Response)
    if (xyzmoEnrollDynamicProfileResponse.getBaseResult eq WebServiceBiometricPartStub.BaseResultEnum.ok.getValue) {
      val enrollResult = xyzmoEnrollDynamicProfileResponse.getEnrollResult
      log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult.getOrElse(None))
      enrollResult.getOrElse(None) match {
        case "EnrollCompleted" => {
          log.info("EnrollDynamicProfile_v1: Profile " + xyzmoEnrollDynamicProfileResponse.getProfileId.getOrElse(None) + " created; contains " + xyzmoEnrollDynamicProfileResponse.getNrEnrolled.getOrElse(None) + " signatures.")
        }
        case "EnrollRejected" => {
          log.info(xyzmoEnrollDynamicProfileResponse.getRejectedSignaturesSummary.getOrElse(None))
        }
        case "EnrollContinued:" => {
          // This should never happen.
        }
      }
    }
    else {
      log.error("Error during EnrollDynamicProfile_v1: " + xyzmoEnrollDynamicProfileResponse.getErrorMsg.getOrElse(None))
    }
    println("enrollDynamicProfile_v1Response " + enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo)
    xyzmoEnrollDynamicProfileResponse
  }

  def verifyUser(userId: String, signatureDCToVerify: String): XyzmoVerifyUserResponse = {
    val verifyUserBySignatureDynamicToDynamic_v1: VerifyUserBySignatureDynamicToDynamic_v1 = new VerifyUserBySignatureDynamicToDynamic_v1
    verifyUserBySignatureDynamicToDynamic_v1.setBioUserId(userId)
    verifyUserBySignatureDynamicToDynamic_v1.setSignatureDataContainerXmlStr(signatureDCToVerify)
    val webServiceBiometricPart: Option[WebServiceBiometricPartStub] = getWebServiceBiometricPartStub
    val verifyUserBySignatureDynamicToDynamic_v1Response: VerifyUserBySignatureDynamicToDynamic_v1Response = webServiceBiometricPart.get.verifyUserBySignatureDynamicToDynamic_v1(verifyUserBySignatureDynamicToDynamic_v1)
    val xyzmoVerifyUserResponse: XyzmoVerifyUserResponse = new XyzmoVerifyUserResponse(verifyUserBySignatureDynamicToDynamic_v1Response)
    xyzmoVerifyUserResponse
  }

  def getSignatureDataContainerFromJSON(jsonStr: String): String = {
    val json: GetSignatureDataContainerFromJSON = new GetSignatureDataContainerFromJSON()
    json.setJsonData(jsonStr)
    val sdcFromJSON: Option[SDCFromJSONStub] = getSDCFromJSON
    val getSignatureDataContainerFromJSONResponse: GetSignatureDataContainerFromJSONResponse = sdcFromJSON.get.getSignatureDataContainerFromJSON(json)
    getSignatureDataContainerFromJSONResponse.getGetSignatureDataContainerFromJSONResult
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

abstract class XyzmoUserAndProfileResponse(val userAndProfileResultBase: WebServiceUserAndProfileStub.ResultBase) {
  def getBaseResult: String = {
    userAndProfileResultBase.getBaseResult.getValue
  }

  def getError: Option[String] = {
    val errorInfo = userAndProfileResultBase.getErrorInfo
    if (errorInfo != null) Some(errorInfo.getError.getValue) else None
  }

  def getErrorMsg: Option[String] = {
    val errorInfo = userAndProfileResultBase.getErrorInfo
    if (errorInfo != null) Some(errorInfo.getErrorMsg) else None
  }

  override def toString = List("BaseResult: " + getBaseResult, "Error: " + getError, "ErrorMsg: " + getErrorMsg).mkString(", ")
}

class XyzmoAddUserResponse(val user_Add_v1Response: User_Add_v1Response)
  extends XyzmoUserAndProfileResponse(user_Add_v1Response.getUser_Add_v1Result)

class XyzmoDeleteUserResponse(val user_Delete_v1Response: User_Delete_v1Response)
  extends XyzmoUserAndProfileResponse(user_Delete_v1Response.getUser_Delete_v1Result)

class XyzmoAddProfileResponse(val profile_Add_v1Response: Profile_Add_v1Response)
  extends XyzmoUserAndProfileResponse(profile_Add_v1Response.getProfile_Add_v1Result) {

  def getXyzmoProfileId: Option[String] = {
    val okInfo = profile_Add_v1Response.getProfile_Add_v1Result.getOkInfo
    val profileInfo = if (okInfo != null) Some(okInfo.getProfileInfo) else None
    if (profileInfo.isDefined) Some(profileInfo.get.getProfileId) else None
  }

  override def toString = List(super.toString, "XyzmoProfileId: " + getXyzmoProfileId).mkString(", ")
}

abstract class XyzmoBiometricPartResponse(val biometricPartResultBase: WebServiceBiometricPartStub.ResultBase) {
  def getBaseResult: String = {
    biometricPartResultBase.getBaseResult.getValue
  }

  def getError: Option[String] = {
    val errorInfo = biometricPartResultBase.getErrorInfo
    if (errorInfo != null) Some(errorInfo.getError.getValue) else None
  }

  def getErrorMsg: Option[String] = {
    val errorInfo = biometricPartResultBase.getErrorInfo
    if (errorInfo != null) Some(errorInfo.getErrorMsg) else None
  }

  override def toString = List("BaseResult: " + getBaseResult, "Error: " + getError, "ErrorMsg: " + getErrorMsg).mkString(", ")
}

class XyzmoEnrollDynamicProfileResponse(val enrollDynamicProfile_v1Response: EnrollDynamicProfile_v1Response)
  extends XyzmoBiometricPartResponse(enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result) {

  def isSuccessfulSignatureEnrollment: Boolean = {
    getEnrollResult.getOrElse(None) == EnrollResultEnum.EnrollCompleted.getValue
  }

  def isProfileAlreadyEnrolled: Boolean = {
    getError.getOrElse(None) == WebServiceBiometricPartStub.ErrorStatus.ProfileAlreadyEnrolled.getValue
  }

  def getEnrollResult: Option[String] = {
    val okInfo = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo
    if (okInfo != null) Some(okInfo.getEnrollResult.getValue) else None
  }

  def getProfileId: Option[String] = {
    val okInfo = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo
    val infoEnrollOk = if (okInfo != null) Some(okInfo.getInfoEnrollOk) else None
    if (infoEnrollOk.isDefined) Some(infoEnrollOk.get.getProfileId) else None
  }

  def getNrEnrolled: Option[Int] = {
    val okInfo = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo
    val infoEnrollOk = if (okInfo != null) Some(okInfo.getInfoEnrollOk) else None
    if (infoEnrollOk.isDefined) Some(infoEnrollOk.get.getNrEnrolled) else None
  }

  def getRejectedSignaturesSummary: Option[String] = {
    val okInfo = enrollDynamicProfile_v1Response.getEnrollDynamicProfile_v1Result.getOkInfo
    val rejectedSignaturesSOAPObject = if (okInfo != null) Some(okInfo.getRejectedSignatures) else None
    val rejectedSignatureArray = if (rejectedSignaturesSOAPObject.isDefined) Some(rejectedSignaturesSOAPObject.get.getRejectedSignature) else None

    if (rejectedSignatureArray.isDefined) {
      if (rejectedSignatureArray.get != null && rejectedSignatureArray.get.size > 0) {
        var rejectedSignaturesSummary: String = rejectedSignatureArray.get + " signature rejected. Reasons: "
        for (rejectedSignature <- rejectedSignatureArray.get) {
          val index: Int = rejectedSignature.getIndex
          val reasonString: String = rejectedSignature.getReason.toString
          rejectedSignaturesSummary += index + ") " + reasonString + ". "
        }
        // todo(wchan): Test this path in SignatureBiometricServicesTraitTests
        return Some(rejectedSignaturesSummary)
      }
    }

    None
  }

  override def toString = List(super.toString, "EnrollResult: " + getEnrollResult, "ProfileId: " + getProfileId, "NrEnrolled: " + getNrEnrolled,
    "isSuccessfulSignatureEnrollment: " + isSuccessfulSignatureEnrollment, "isProfileAlreadyEnrolled: " + isProfileAlreadyEnrolled,
    "getRejectedSignaturesSummary: " + getRejectedSignaturesSummary).mkString(", ")
}

class XyzmoVerifyUserResponse(val verifyUserBySignatureDynamicToDynamic_v1Response: VerifyUserBySignatureDynamicToDynamic_v1Response)
  extends XyzmoBiometricPartResponse(verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result) {

  def isMatch: Boolean = {
    val okInfo = verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result.getOkInfo
    if (okInfo != null) (okInfo.getVerifyResult.getValue == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch.getValue) else false
  }

  def getScore: Option[Int] = {
    val okInfo = verifyUserBySignatureDynamicToDynamic_v1Response.getVerifyUserBySignatureDynamicToDynamic_v1Result.getOkInfo
    if (okInfo != null) Some(okInfo.getScore) else None
  }

  override def toString = List(super.toString, "isMatch: " + isMatch, "Score: " + getScore).mkString(", ")
}
