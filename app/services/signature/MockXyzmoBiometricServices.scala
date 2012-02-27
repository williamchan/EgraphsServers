package services.signature

import org.apache.log4j.Logger
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.{Profile_Add_v1Response, User_Delete_v1Response, User_Add_v1Response}
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._
import com.xyzmo.wwww.biometricserver.{WebServiceBiometricPartStub, WebServiceUserAndProfileStub}

// todo(wchan): This is ready to use in place of skipBiometrics
object MockXyzmoBiometricServices extends XyzmoBiometricServicesBase {

  protected val log: Logger = Logger.getLogger(MockXyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = false
  protected val domain: String = ""
  protected val host: String = ""

  override def addUser(userId: String, userName: String): XyzmoAddUserResponse = {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)

    val user_Add_v1Response: User_Add_v1Response = new User_Add_v1Response
    user_Add_v1Response.setUser_Add_v1Result(resultBase)
    new XyzmoAddUserResponse(user_Add_v1Response: User_Add_v1Response)
  }

  override def deleteUser(userId: String): XyzmoDeleteUserResponse = {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)

    val user_Delete_v1Response: User_Delete_v1Response = new User_Delete_v1Response
    user_Delete_v1Response.setUser_Delete_v1Result(resultBase)
    new XyzmoDeleteUserResponse(user_Delete_v1Response: User_Delete_v1Response)
  }

  override def addProfile(userId: String, profileName: String): XyzmoAddProfileResponse = {
    val profileInfoResult_v1 = new WebServiceUserAndProfileStub.ProfileInfoResult_v1
    profileInfoResult_v1.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)
    val profileResult = new WebServiceUserAndProfileStub.ProfileResult
    val profileInfo = new WebServiceUserAndProfileStub.ProfileInfo
    profileInfo.setProfileId("profile" + userId)
    profileResult.setProfileInfo(profileInfo)
    profileInfoResult_v1.setOkInfo(profileResult)

    val profile_Add_v1Response: Profile_Add_v1Response = new Profile_Add_v1Response
    profile_Add_v1Response.setProfile_Add_v1Result(profileInfoResult_v1)
    new XyzmoAddProfileResponse(profile_Add_v1Response: Profile_Add_v1Response)
  }

  override def enrollUser(userId: String, profileName: String, signatureDataContainers: List[String]): XyzmoEnrollDynamicProfileResponse = {
    val enrollResultInfo_v1 = new EnrollResultInfo_v1
    enrollResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.ok)
    val enrollResult = new EnrollResult
    enrollResult.setEnrollResult(EnrollResultEnum.EnrollCompleted)
    val enrollOkInfo = new EnrollOkInfo
    enrollOkInfo.setProfileId("profile" + userId)
    enrollOkInfo.setNrEnrolled(6)
    enrollResult.setInfoEnrollOk(enrollOkInfo)
    val arrayOfRejectedSignature = new ArrayOfRejectedSignature
    enrollResult.setRejectedSignatures(arrayOfRejectedSignature)
    enrollResultInfo_v1.setOkInfo(enrollResult)

    val enrollDynamicProfile_v1Response: EnrollDynamicProfile_v1Response = new EnrollDynamicProfile_v1Response
    enrollDynamicProfile_v1Response.setEnrollDynamicProfile_v1Result(enrollResultInfo_v1)
    new XyzmoEnrollDynamicProfileResponse(enrollDynamicProfile_v1Response: EnrollDynamicProfile_v1Response)
  }

  override def verifyUser(userId: String, signatureDCToVerify: String): XyzmoVerifyUserResponse = {
    val verifyResultInfo_v1 = new VerifyResultInfo_v1
    verifyResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.ok)
    val verifyResult = new VerifyResult
    val verifyMatchInfo = new VerifyMatchInfo
    verifyResult.setInfoVerifyMatch(verifyMatchInfo)
    verifyResult.setVerifyResult(VerifyResultEnum.VerifyMatch)
    verifyResult.setScore(100)
    verifyResultInfo_v1.setOkInfo(verifyResult)

    val verifyUserBySignatureDynamicToDynamic_v1Response: VerifyUserBySignatureDynamicToDynamic_v1Response = new VerifyUserBySignatureDynamicToDynamic_v1Response
    verifyUserBySignatureDynamicToDynamic_v1Response.setVerifyUserBySignatureDynamicToDynamic_v1Result(verifyResultInfo_v1)
    new XyzmoVerifyUserResponse(verifyUserBySignatureDynamicToDynamic_v1Response: VerifyUserBySignatureDynamicToDynamic_v1Response)
  }

  override def getSignatureDataContainerFromJSON(jsonStr: String): String = {
    "Not Implemented"
  }

}
