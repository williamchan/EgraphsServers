package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub
import models.{EnrollmentBatch, Celebrity}

class XyzmoEnrollDynamicProfileTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoEnrollDynamicProfile]
with CreatedUpdatedEntityTests[XyzmoEnrollDynamicProfile]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoEnrollDynamicProfile] methods
  //

  val store = AppConfig.instance[XyzmoEnrollDynamicProfileStore]

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate base fields" in {
    val enrollDynamicProfile_v1Response = new WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response
    val enrollResultInfo_v1 = new WebServiceBiometricPartStub.EnrollResultInfo_v1
    enrollDynamicProfile_v1Response.setEnrollDynamicProfile_v1Result(enrollResultInfo_v1)
    enrollResultInfo_v1.setBaseResult(WebServiceBiometricPartStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceBiometricPartStub.ErrorInfo
    errorInfo.setError(WebServiceBiometricPartStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    enrollResultInfo_v1.setErrorInfo(errorInfo)

    val xyzmoEnrollDynamicProfile: XyzmoEnrollDynamicProfile = newEntity.withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response)
    xyzmoEnrollDynamicProfile.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.failed.getValue)
    xyzmoEnrollDynamicProfile.error.get should be(WebServiceBiometricPartStub.ErrorStatus.ArgumentError.getValue)
    xyzmoEnrollDynamicProfile.errorMsg.get should be("omg")
    xyzmoEnrollDynamicProfile.enrollResult should be(None)
    xyzmoEnrollDynamicProfile.xyzmoProfileId should be(None)
    xyzmoEnrollDynamicProfile.nrEnrolled should be(None)
    xyzmoEnrollDynamicProfile.rejectedSignaturesSummary should be(None)
    xyzmoEnrollDynamicProfile.enrollmentSampleIds should be(None)
    xyzmoEnrollDynamicProfile.isSuccessfulSignatureEnrollment should be(false)
    xyzmoEnrollDynamicProfile.isProfileAlreadyEnrolled should be(false)
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

    val xyzmoEnrollDynamicProfile: XyzmoEnrollDynamicProfile = newEntity.withEnrollDynamicProfile_v1Response(enrollDynamicProfile_v1Response)
    xyzmoEnrollDynamicProfile.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoEnrollDynamicProfile.error should be(None)
    xyzmoEnrollDynamicProfile.errorMsg should be(None)
    xyzmoEnrollDynamicProfile.enrollResult.get should be(WebServiceBiometricPartStub.EnrollResultEnum.EnrollCompleted.getValue)
    xyzmoEnrollDynamicProfile.xyzmoProfileId.get should be("profile")
    xyzmoEnrollDynamicProfile.nrEnrolled.get should be(6)
    xyzmoEnrollDynamicProfile.rejectedSignaturesSummary should be(None)
    xyzmoEnrollDynamicProfile.enrollmentSampleIds should be(None)
    xyzmoEnrollDynamicProfile.isSuccessfulSignatureEnrollment should be(true)
    xyzmoEnrollDynamicProfile.isProfileAlreadyEnrolled should be(false)
  }

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate enrollmentSampleIds" in {
    // todo(wchan): write this test
  }

  "withVerifyUserBySignatureDynamicToDynamic_v1Response" should "populate rejectedSignaturesSummary" in {
    // todo(wchan): write this test
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = new Celebrity().save().id).save()
    new XyzmoEnrollDynamicProfile(enrollmentBatchId = enrollmentBatch.id)
  }

  def saveEntity(toSave: XyzmoEnrollDynamicProfile) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoEnrollDynamicProfile) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
