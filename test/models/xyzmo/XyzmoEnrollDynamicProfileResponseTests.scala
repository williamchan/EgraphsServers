package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub

class XyzmoEnrollDynamicProfileResponseTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoEnrollDynamicProfileResponse]
with CreatedUpdatedEntityTests[XyzmoEnrollDynamicProfileResponse]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoEnrollDynamicProfileResponse] methods
  //

  val store = AppConfig.instance[XyzmoEnrollDynamicProfileResponseStore]

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate base fields" in {
    val enrollDynamicProfile_v1Response = new WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response
    val enrollResultInfo_v1 = new WebServiceBiometricPartStub.EnrollResultInfo_v1
    enrollDynamicProfile_v1Response.setEnrollDynamicProfile_v1Result(enrollResultInfo_v1)
    enrollResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceBiometricPartStub.ErrorInfo
    errorInfo.setError(WebServiceBiometricPartStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    enrollResultInfo_v1.setErrorInfo(errorInfo)

    val xyzmoEnrollDynamicProfileResponse: XyzmoEnrollDynamicProfileResponse = newEntity.withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response)
    xyzmoEnrollDynamicProfileResponse.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.failed.getValue)
    xyzmoEnrollDynamicProfileResponse.error.get should be(WebServiceBiometricPartStub.ErrorStatus.ArgumentError.getValue)
    xyzmoEnrollDynamicProfileResponse.errorMsg.get should be("omg")
    xyzmoEnrollDynamicProfileResponse.enrollResult should be(None)
    xyzmoEnrollDynamicProfileResponse.xyzmoProfileId should be(None)
    xyzmoEnrollDynamicProfileResponse.nrEnrolled should be(None)
    xyzmoEnrollDynamicProfileResponse.rejectedSignaturesSummary should be(None)
    xyzmoEnrollDynamicProfileResponse.enrollmentSampleIds should be(None)
    xyzmoEnrollDynamicProfileResponse.isSuccessfulSignatureEnrollment should be(false)
    xyzmoEnrollDynamicProfileResponse.isProfileAlreadyEnrolled should be(false)
  }

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate result fields" in {
    val enrollDynamicProfile_v1Response = new WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response
    val enrollResultInfo_v1 = new WebServiceBiometricPartStub.EnrollResultInfo_v1
    enrollDynamicProfile_v1Response.setEnrollDynamicProfile_v1Result(enrollResultInfo_v1)
    enrollResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.ok)
    val enrollResult = new WebServiceBiometricPartStub.EnrollResult
    enrollResult.setEnrollResult(WebServiceBiometricPartStub.EnrollResultEnum.EnrollCompleted)
    val enrollOkInfo = new WebServiceBiometricPartStub.EnrollOkInfo
    enrollOkInfo.setProfileId("profile")
    enrollOkInfo.setNrEnrolled(6)
    enrollResult.setInfoEnrollOk(enrollOkInfo)
    val arrayOfRejectedSignature = new WebServiceBiometricPartStub.ArrayOfRejectedSignature
    enrollResult.setRejectedSignatures(arrayOfRejectedSignature)
    enrollResultInfo_v1.setOkInfo(enrollResult)

    val xyzmoEnrollDynamicProfileResponse: XyzmoEnrollDynamicProfileResponse = newEntity.withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response)
    xyzmoEnrollDynamicProfileResponse.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoEnrollDynamicProfileResponse.error should be(None)
    xyzmoEnrollDynamicProfileResponse.errorMsg should be(None)
    xyzmoEnrollDynamicProfileResponse.enrollResult.get should be(WebServiceBiometricPartStub.EnrollResultEnum.EnrollCompleted.getValue)
    xyzmoEnrollDynamicProfileResponse.xyzmoProfileId.get should be("profile")
    xyzmoEnrollDynamicProfileResponse.nrEnrolled.get should be(6)
    xyzmoEnrollDynamicProfileResponse.rejectedSignaturesSummary should be(None)
    xyzmoEnrollDynamicProfileResponse.enrollmentSampleIds should be(None)
    xyzmoEnrollDynamicProfileResponse.isSuccessfulSignatureEnrollment should be(true)
    xyzmoEnrollDynamicProfileResponse.isProfileAlreadyEnrolled should be(false)
  }

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate enrollmentSampleIds" in {
    // todo(wchan): write this test
  }

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate rejectedSignaturesSummary" in {
    // todo(wchan): write this test
  }

  def newEntity = {
    new XyzmoEnrollDynamicProfileResponse()
  }

  def saveEntity(toSave: XyzmoEnrollDynamicProfileResponse) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoEnrollDynamicProfileResponse) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
