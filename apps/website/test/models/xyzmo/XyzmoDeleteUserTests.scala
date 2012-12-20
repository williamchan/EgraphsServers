package models.xyzmo

import utils._
import services.AppConfig
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub
import models.{EnrollmentBatch, Celebrity}

class XyzmoDeleteUserTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[XyzmoDeleteUser]
  with CreatedUpdatedEntityTests[Long, XyzmoDeleteUser]
  with DateShouldMatchers
  with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoDeleteUser] methods
  //

  def store = AppConfig.instance[XyzmoDeleteUserStore]

  "withResultBase" should "populate base fields" in new EgraphsTestApplication {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceUserAndProfileStub.ErrorInfo
    errorInfo.setError(WebServiceUserAndProfileStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    resultBase.setErrorInfo(errorInfo)

    val xyzmoDeleteUser: XyzmoDeleteUser = newEntity.withResultBase(resultBase)
    xyzmoDeleteUser.baseResult should be (WebServiceUserAndProfileStub.BaseResultEnum.failed.getValue)
    xyzmoDeleteUser.error.get should be (WebServiceUserAndProfileStub.ErrorStatus.ArgumentError.getValue)
    xyzmoDeleteUser.errorMsg.get should be ("omg")
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
    new XyzmoDeleteUser(enrollmentBatchId = enrollmentBatch.id, baseResult = "ok")
  }

  def saveEntity(toSave: XyzmoDeleteUser) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoDeleteUser) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
