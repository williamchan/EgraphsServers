package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.Celebrity
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub

class XyzmoAddProfileTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoAddProfile]
with CreatedUpdatedEntityTests[XyzmoAddProfile]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoAddProfile] methods
  //

  val store = AppConfig.instance[XyzmoAddProfileStore]

  "withProfile_Add_v1Response" should "populate base fields" in {
    val profile_Add_v1Response = new WebServiceUserAndProfileStub.Profile_Add_v1Response
    val profileInfoResult_v1 = new WebServiceUserAndProfileStub.ProfileInfoResult_v1
    profile_Add_v1Response.setProfile_Add_v1Result(profileInfoResult_v1)
    profileInfoResult_v1.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceUserAndProfileStub.ErrorInfo
    errorInfo.setError(WebServiceUserAndProfileStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    profileInfoResult_v1.setErrorInfo(errorInfo)

    val xyzmoAddProfile: XyzmoAddProfile = newEntity.withProfile_Add_v1Response(profile_Add_v1Response)
    xyzmoAddProfile.baseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.failed.getValue)
    xyzmoAddProfile.error.get should be(WebServiceUserAndProfileStub.ErrorStatus.ArgumentError.getValue)
    xyzmoAddProfile.errorMsg.get should be("omg")
    xyzmoAddProfile.xyzmoProfileId should be(None)
  }

  "withProfile_Add_v1Response" should "populate xyzmoProfileId" in {
    val profile_Add_v1Response = new WebServiceUserAndProfileStub.Profile_Add_v1Response
    val profileInfoResult_v1 = new WebServiceUserAndProfileStub.ProfileInfoResult_v1
    profile_Add_v1Response.setProfile_Add_v1Result(profileInfoResult_v1)
    profileInfoResult_v1.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.ok)
    val profileResult = new WebServiceUserAndProfileStub.ProfileResult
    val profileInfo = new WebServiceUserAndProfileStub.ProfileInfo
    profileInfo.setProfileId("profile")
    profileResult.setProfileInfo(profileInfo)
    profileInfoResult_v1.setOkInfo(profileResult)

    val xyzmoAddProfile: XyzmoAddProfile = newEntity.withProfile_Add_v1Response(profile_Add_v1Response)
    xyzmoAddProfile.baseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    xyzmoAddProfile.error should be(None)
    xyzmoAddProfile.errorMsg should be(None)
    xyzmoAddProfile.xyzmoProfileId.get should be("profile")
  }

  def newEntity = {
    val celebrity = Celebrity().save()
    new XyzmoAddProfile(celebrityId = celebrity.id, baseResult = "ok")
  }

  def saveEntity(toSave: XyzmoAddProfile) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoAddProfile) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
