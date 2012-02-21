package services.signature

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub._

class SignatureBiometricServicesTraitTests extends UnitFlatSpec with ShouldMatchers {

  it should "test" in {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(BaseResultEnum.ok)

    val user_Add_v1Response = new User_Add_v1Response()
    user_Add_v1Response.setUser_Add_v1Result(resultBase)
    val xyzmoUserAndProfileResponse0: XyzmoAddUserResponse = new XyzmoAddUserResponse(user_Add_v1Response)
    println("xyzmoUserAndProfileResponse0.getBaseResult " + xyzmoUserAndProfileResponse0.getBaseResult)
    println(xyzmoUserAndProfileResponse0.getError)
    println(xyzmoUserAndProfileResponse0.getErrorMsg)

    val user_Delete_v1Response = new User_Delete_v1Response()
    user_Delete_v1Response.setUser_Delete_v1Result(resultBase)
    val xyzmoUserAndProfileResponse1: XyzmoDeleteUserResponse = new XyzmoDeleteUserResponse(user_Delete_v1Response)
    println("xyzmoUserAndProfileResponse1.getBaseResult " + xyzmoUserAndProfileResponse1.getBaseResult)
    println(xyzmoUserAndProfileResponse1.getError)
    println(xyzmoUserAndProfileResponse1.getErrorMsg)

    val z = new ProfileInfoResult_v1()
    z.setBaseResult(BaseResultEnum.ok)
    val profileResult = new ProfileResult()
    val profileInfo = new ProfileInfo()
    profileInfo.setProfileId("myId")
    profileResult.setProfileInfo(profileInfo)
    z.setOkInfo(profileResult)
    val profile_Add_v1Response = new Profile_Add_v1Response()
    profile_Add_v1Response.setProfile_Add_v1Result(z)
    val xyzmoUserAndProfileResponse2: XyzmoAddProfileResponse = new XyzmoAddProfileResponse(profile_Add_v1Response)
    println("xyzmoUserAndProfileResponse2.getBaseResult " + xyzmoUserAndProfileResponse2.getBaseResult)
    println(xyzmoUserAndProfileResponse2.getError)
    println(xyzmoUserAndProfileResponse2.getErrorMsg)
    println(xyzmoUserAndProfileResponse2.getProfileId)

    1 should be(1)
  }

}
