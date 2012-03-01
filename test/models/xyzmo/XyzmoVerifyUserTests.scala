package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub

class XyzmoVerifyUserTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoVerifyUser]
with CreatedUpdatedEntityTests[XyzmoVerifyUser]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoVerifyUser] methods
  //

  val store = AppConfig.instance[XyzmoVerifyUserStore]

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate base fields" in {
    val verifyUserBySignatureDynamicToDynamic_v1Response = new WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1Response
    val verifyResultInfo_v1 = new WebServiceBiometricPartStub.VerifyResultInfo_v1
    verifyUserBySignatureDynamicToDynamic_v1Response.setVerifyUserBySignatureDynamicToDynamic_v1Result(verifyResultInfo_v1)
    verifyResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceBiometricPartStub.ErrorInfo
    errorInfo.setError(WebServiceBiometricPartStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    verifyResultInfo_v1.setErrorInfo(errorInfo)

    val xyzmoVerifyUser: XyzmoVerifyUser = newEntity.withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response)
    xyzmoVerifyUser.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.failed.getValue)
    xyzmoVerifyUser.error.get should be(WebServiceBiometricPartStub.ErrorStatus.ArgumentError.getValue)
    xyzmoVerifyUser.errorMsg.get should be("omg")
    xyzmoVerifyUser.isMatch should be(None)
    xyzmoVerifyUser.score should be(None)
  }

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate isMatch and score" in {
    val verifyUserBySignatureDynamicToDynamic_v1Response = new WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1Response
    val verifyResultInfo_v1 = new WebServiceBiometricPartStub.VerifyResultInfo_v1
    verifyUserBySignatureDynamicToDynamic_v1Response.setVerifyUserBySignatureDynamicToDynamic_v1Result(verifyResultInfo_v1)
    verifyResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.failed)
    verifyResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.ok)
    val verifyResult = new WebServiceBiometricPartStub.VerifyResult
    val verifyMatchInfo = new WebServiceBiometricPartStub.VerifyMatchInfo
    verifyResult.setInfoVerifyMatch(verifyMatchInfo)
    verifyResult.setVerifyResult(WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch)
    verifyResult.setScore(100)
    verifyResultInfo_v1.setOkInfo(verifyResult)

    val xyzmoVerifyUser: XyzmoVerifyUser = newEntity.withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response)
    xyzmoVerifyUser.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoVerifyUser.error should be(None)
    xyzmoVerifyUser.errorMsg should be(None)
    xyzmoVerifyUser.isMatch.get should be(true)
    xyzmoVerifyUser.score.get should be(100)
  }

  def newEntity = {
    new XyzmoVerifyUser()
  }

  def saveEntity(toSave: XyzmoVerifyUser) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoVerifyUser) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}