package models.xyzmo

import utils._
import services.AppConfig
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub
import models.{EnrollmentBatch, Celebrity}

class XyzmoAddUserTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[XyzmoAddUser]
  with CreatedUpdatedEntityTests[Long, XyzmoAddUser]
  with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoAddUser] methods
  //

  val store = AppConfig.instance[XyzmoAddUserStore]

  "withResultBase" should "populate base fields" in {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceUserAndProfileStub.ErrorInfo
    errorInfo.setError(WebServiceUserAndProfileStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    resultBase.setErrorInfo(errorInfo)

    val xyzmoAddUser: XyzmoAddUser = newEntity.withResultBase(resultBase)
    xyzmoAddUser.baseResult should be (WebServiceUserAndProfileStub.BaseResultEnum.failed.getValue)
    xyzmoAddUser.error.get should be (WebServiceUserAndProfileStub.ErrorStatus.ArgumentError.getValue)
    xyzmoAddUser.errorMsg.get should be ("omg")
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
    new XyzmoAddUser(enrollmentBatchId = enrollmentBatch.id, baseResult = "ok")
  }

  def saveEntity(toSave: XyzmoAddUser) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoAddUser) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
