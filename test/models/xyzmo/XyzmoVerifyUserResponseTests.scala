package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub

class XyzmoVerifyUserResponseTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoVerifyUserResponse]
with CreatedUpdatedEntityTests[XyzmoVerifyUserResponse]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoVerifyUserResponse] methods
  //

  val store = AppConfig.instance[XyzmoVerifyUserResponseStore]

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate base fields" in {
    val verifyUserBySignatureDynamicToDynamic_v1Response = new WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1Response
    val verifyResultInfo_v1 = new WebServiceBiometricPartStub.VerifyResultInfo_v1
    verifyUserBySignatureDynamicToDynamic_v1Response.setVerifyUserBySignatureDynamicToDynamic_v1Result(verifyResultInfo_v1)
    verifyResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceBiometricPartStub.ErrorInfo
    errorInfo.setError(WebServiceBiometricPartStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    verifyResultInfo_v1.setErrorInfo(errorInfo)

    val xyzmoVerifyUserResponse: XyzmoVerifyUserResponse = newEntity.withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response)
    xyzmoVerifyUserResponse.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.failed.getValue)
    xyzmoVerifyUserResponse.error.get should be(WebServiceBiometricPartStub.ErrorStatus.ArgumentError.getValue)
    xyzmoVerifyUserResponse.errorMsg.get should be("omg")
    xyzmoVerifyUserResponse.isMatch should be(None)
    xyzmoVerifyUserResponse.score should be(None)
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

    val xyzmoVerifyUserResponse: XyzmoVerifyUserResponse = newEntity.withVerifyUserBySignatureDynamicToDynamic_v1Response(verifyUserBySignatureDynamicToDynamic_v1Response)
    xyzmoVerifyUserResponse.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoVerifyUserResponse.error should be(None)
    xyzmoVerifyUserResponse.errorMsg should be(None)
    xyzmoVerifyUserResponse.isMatch.get should be(true)
    xyzmoVerifyUserResponse.score.get should be(100)
  }

  def newEntity = {
    new XyzmoVerifyUserResponse()
  }

  def saveEntity(toSave: XyzmoVerifyUserResponse) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoVerifyUserResponse) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
