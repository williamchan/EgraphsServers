package services.signature

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub.EnrollResultEnum

class SignatureBiometricServicesTraitTests extends UnitFlatSpec with ShouldMatchers {

  private val testuserid = "testuserid"
  private val testusername = "testusername"
  private val profileId = testuserid + testusername

  it should "test XyzmoAddUserResponse" in {
    val addUserResponse: XyzmoAddUserResponse = MockXyzmoBiometricServices.addUser(testuserid, testusername)
    addUserResponse.getBaseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    addUserResponse.getError should be(None)
    addUserResponse.getErrorMsg should be(None)
  }

  it should "test XyzmoDeleteUserResponse" in {
    val deleteUserResponse: XyzmoDeleteUserResponse = MockXyzmoBiometricServices.deleteUser(testuserid)
    deleteUserResponse.getBaseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    deleteUserResponse.getError should be(None)
    deleteUserResponse.getErrorMsg should be(None)
  }

  it should "test XyzmoAddProfileResponse" in {
    val addProfileResponse: XyzmoAddProfileResponse = MockXyzmoBiometricServices.addProfile(testuserid, testusername)
    addProfileResponse.getBaseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    addProfileResponse.getError should be(None)
    addProfileResponse.getErrorMsg should be(None)
    addProfileResponse.getProfileId.get should be("profile" + testuserid)
  }

  it should "test XyzmoEnrollDynamicProfileResponse" in {
    val enrollDynamicProfileResponse: XyzmoEnrollDynamicProfileResponse = MockXyzmoBiometricServices.enrollUser(testuserid, profileId, List.empty)
    enrollDynamicProfileResponse.getBaseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    enrollDynamicProfileResponse.getError should be(None)
    enrollDynamicProfileResponse.getErrorMsg should be(None)
    enrollDynamicProfileResponse.getProfileId.get should be("profile" + testuserid)
    enrollDynamicProfileResponse.getEnrollResult.get should be(EnrollResultEnum.EnrollCompleted.getValue)
    enrollDynamicProfileResponse.getNrEnrolled.get should be(6)
    enrollDynamicProfileResponse.getRejectedSignaturesSummary should be(None)
  }

  it should "test XyzmoVerifyUserResponse" in {
    val verifyUserResponse: XyzmoVerifyUserResponse = MockXyzmoBiometricServices.verifyUser(testuserid, "")
    verifyUserResponse.getBaseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    verifyUserResponse.getError should be(None)
    verifyUserResponse.getErrorMsg should be(None)
    verifyUserResponse.getScore.get should be(100)
    verifyUserResponse.isMatch should be(true)
  }
}
