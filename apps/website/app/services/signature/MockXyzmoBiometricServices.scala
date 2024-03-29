package services.signature

import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.{Profile_Add_v1Response, User_Delete_v1Response, User_Add_v1Response}
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub._
import com.xyzmo.wwww.biometricserver.{WebServiceBiometricPartStub, WebServiceUserAndProfileStub}
import models.xyzmo._
import org.slf4j.LoggerFactory
import models.{Egraph, EnrollmentBatch}

object MockXyzmoBiometricServices extends XyzmoBiometricServicesBase {

  override protected val log = LoggerFactory.getLogger(MockXyzmoBiometricServices.getClass)
  override protected val isBasicAuth = false
  override protected val domain = ""
  override protected val host = ""
  override protected val _userIdPrefix = "mock"

  override protected[signature] def addUser(enrollmentBatch: EnrollmentBatch): XyzmoAddUser = {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)

    val user_Add_v1Response: User_Add_v1Response = new User_Add_v1Response
    user_Add_v1Response.setUser_Add_v1Result(resultBase)
    new XyzmoAddUser().withResultBase(resultBase)
  }

  override protected[signature] def deleteUser(enrollmentBatch: EnrollmentBatch): XyzmoDeleteUser = {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)

    val user_Delete_v1Response: User_Delete_v1Response = new User_Delete_v1Response
    user_Delete_v1Response.setUser_Delete_v1Result(resultBase)
    new XyzmoDeleteUser().withResultBase(resultBase)
  }

  override protected[signature] def addProfile(enrollmentBatch: EnrollmentBatch): XyzmoAddProfile = {
    val profileInfoResult_v1 = new WebServiceUserAndProfileStub.ProfileInfoResult_v1
    profileInfoResult_v1.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)
    val profileResult = new WebServiceUserAndProfileStub.ProfileResult
    val profileInfo = new WebServiceUserAndProfileStub.ProfileInfo
    profileInfo.setProfileId("profile" + enrollmentBatch.celebrityId)
    profileResult.setProfileInfo(profileInfo)
    profileInfoResult_v1.setOkInfo(profileResult)

    val profile_Add_v1Response: Profile_Add_v1Response = new Profile_Add_v1Response
    profile_Add_v1Response.setProfile_Add_v1Result(profileInfoResult_v1)
    new XyzmoAddProfile().withProfile_Add_v1Response(profile_Add_v1Response)
  }

  override protected[signature] def enrollUser(enrollmentBatch: EnrollmentBatch, signatureDataContainers: List[String]): XyzmoEnrollDynamicProfile = {
    val userId = getUserId(enrollmentBatch.celebrityId)

    val enrollResultInfo_v1 = new EnrollResultInfo_v1
    enrollResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.ok)
    val enrollResult = new EnrollResult
    enrollResult.setEnrollResult(EnrollResultEnum.EnrollCompleted)
    val enrollOkInfo = new EnrollOkInfo
    enrollOkInfo.setProfileId(userId)
    enrollOkInfo.setNrEnrolled(6)
    enrollResult.setInfoEnrollOk(enrollOkInfo)
    val arrayOfRejectedSignature = new ArrayOfRejectedSignature
    enrollResult.setRejectedSignatures(arrayOfRejectedSignature)
    enrollResultInfo_v1.setOkInfo(enrollResult)

    val enrollDynamicProfile_v1Response: EnrollDynamicProfile_v1Response = new EnrollDynamicProfile_v1Response
    enrollDynamicProfile_v1Response.setEnrollDynamicProfile_v1Result(enrollResultInfo_v1)
    new XyzmoEnrollDynamicProfile().withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response)
  }

  override protected[signature] def verifyUser(egraph: Egraph, signatureDCToVerify: String): XyzmoVerifyUser = {
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
    new XyzmoVerifyUser().withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response)
  }

  override def getSignatureDataContainerFromJSON(jsonStr: String): String = {
    "Not Implemented"
  }

}
