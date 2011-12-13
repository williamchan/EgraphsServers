package services.signature

import org.apache.log4j.Logger
import org.apache.axis2.transport.http.{HttpTransportProperties, HTTPConstants}
import com.xyzmo.wwww.biometricserver.{SDCFromJSONStub, WebServiceBiometricPartStub, WebServiceUserAndProfileStub}
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub._
import com.xyzmo.wwww.biometricserver.SDCFromJSONStub.{GetSignatureDataContainerFromJSON, GetSignatureDataContainerFromJSONResponse}
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._


object XyzmoBiometricServices {

  private val log: Logger = Logger.getLogger(XyzmoBiometricServices.getClass)
  private val username: String = "usermanager"
  private val password: String = "%User%01"
  private val domain: String = "testlab"
  private val port: Int = 50200
  private val host: String = "testlab.xyzmo.com"
  private val url: String = "http://testlab.xyzmo.com:50200/WebServices/"

  def getSignatureDataContainerFromJSON(jsonStr: String): GetSignatureDataContainerFromJSONResponse = {
    val json: GetSignatureDataContainerFromJSON = new GetSignatureDataContainerFromJSON
    json.setJsonData(jsonStr)
    val response: GetSignatureDataContainerFromJSONResponse = getSDCFromJSON.getSignatureDataContainerFromJSON(json)
    response
  }

  def addUser(userId: String, userName: String) {
    val user: User_Add_v1 = new User_Add_v1
    user.setBioUserId(userId)
    user.setDisplayName(userName)
    user.setBioUserStatus(BioUserStatus.Active)
    val userAddResponse: User_Add_v1Response = getWebServiceProxyUserAndProfile.get.user_Add_v1(user)
    val user_add_v1Result: WebServiceUserAndProfileStub.ResultBase = userAddResponse.getUser_Add_v1Result
    if (user_add_v1Result.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      XyzmoBiometricServices.log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.")
    }
    else {
      XyzmoBiometricServices.log.error("Error during User_Add_v1: " + user_add_v1Result.getErrorInfo.getErrorMsg)
      if (user_add_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  def addProfile(userId: String, profileName: String) {
    val profile: Profile_Add_v1 = new Profile_Add_v1
    profile.setBioUserId(userId)
    profile.setProfileName(profileName)
    profile.setProfileType(ProfileType.Dynamic)
    val profileAddResponse: Profile_Add_v1Response = getWebServiceProxyUserAndProfile.get.profile_Add_v1(profile)
    val profile_add_v1Result: ProfileInfoResult_v1 = profileAddResponse.getProfile_Add_v1Result
    if (profile_add_v1Result.getBaseResult eq WebServiceUserAndProfileStub.BaseResultEnum.ok) {
      XyzmoBiometricServices.log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.")
    }
    else {
      XyzmoBiometricServices.log.error("Error during Profile_Add_v1: " + profile_add_v1Result.getErrorInfo.getErrorMsg)
      if (profile_add_v1Result.getErrorInfo.getError eq WebServiceUserAndProfileStub.ErrorStatus.BioUserAlreadyExists) {
      }
    }
  }

  // Need to decide how to test addUser+addProfile+enrollUser, then delete this method
  def javatest_EnrollUser(userId: String, profileName: String, signature1: String, signature2: String, signature3: String, signature4: String, signature5: String, signature6: String) {
    enrollUser(userId, profileName, List[String](signature1, signature2, signature3, signature4, signature5, signature6))
  }

  def enrollUser(userId: String, profileName: String, signatureDataContainers: List[String]): Boolean = {
    val SignatureDataContainerXmlStrArr: WebServiceBiometricPartStub.ArrayOfString = new WebServiceBiometricPartStub.ArrayOfString
    for (sdc <- signatureDataContainers) SignatureDataContainerXmlStrArr.addString(sdc)
    val enrollProfileInfo: EnrollDynamicProfile_v1 = new EnrollDynamicProfile_v1
    enrollProfileInfo.setBioUserId(userId)
    enrollProfileInfo.setProfileName(profileName)
    enrollProfileInfo.setContinuous(false)
    enrollProfileInfo.setSignatureDataContainerXmlStrArr(SignatureDataContainerXmlStrArr)
    val enrollDynamicResponse1: EnrollDynamicProfile_v1Response = getWebServiceProxyBiometricPart.get.enrollDynamicProfile_v1(enrollProfileInfo)
    val enrollResult1: EnrollResultInfo_v1 = enrollDynamicResponse1.getEnrollDynamicProfile_v1Result
    if (enrollResult1.getBaseResult eq WebServiceBiometricPartStub.BaseResultEnum.ok) {
      XyzmoBiometricServices.log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult1.getOkInfo.getEnrollResult.getValue)
      XyzmoBiometricServices.log.info("EnrollDynamicProfile_v1: Profile " + enrollResult1.getOkInfo.getInfoEnrollOk.getProfileId + " created; contains " + enrollResult1.getOkInfo.getInfoEnrollOk.getNrEnrolled + " signatures.")
      true
    }
    else {
      XyzmoBiometricServices.log.error("Error during EnrollDynamicProfile_v1: " + enrollResult1.getErrorInfo.getErrorMsg)
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
      val webServiceBiometricPart = new WebServiceBiometricPartStub(url + "/WebServiceBiometricPart.asmx")
      val auth: HttpTransportProperties.Authenticator = new HttpTransportProperties.Authenticator
      auth.setHost(host)
      auth.setPort(port)
      auth.setDomain(domain)
      auth.setUsername(username)
      auth.setPassword(password)
      webServiceBiometricPart._getServiceClient.getOptions.setProperty(HTTPConstants.AUTHENTICATE, auth)
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
      val webServiceUserAndProfile = new WebServiceUserAndProfileStub(url + "/WebServiceUserAndProfile.asmx")
      val auth: HttpTransportProperties.Authenticator = new HttpTransportProperties.Authenticator
      auth.setHost(host)
      auth.setPort(port)
      auth.setDomain(domain)
      auth.setUsername(username)
      auth.setPassword(password)
      webServiceUserAndProfile._getServiceClient.getOptions.setProperty(HTTPConstants.AUTHENTICATE, auth)
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
}