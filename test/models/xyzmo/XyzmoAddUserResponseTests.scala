package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.Celebrity
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub

class XyzmoAddUserResponseTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoAddUserResponse]
with CreatedUpdatedEntityTests[XyzmoAddUserResponse]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoAddUserResponse] methods
  //

  val store = AppConfig.instance[XyzmoAddUserResponseStore]

  "withResultBase" should "populate base fields" in {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceUserAndProfileStub.ErrorInfo
    errorInfo.setError(WebServiceUserAndProfileStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    resultBase.setErrorInfo(errorInfo)

    val xyzmoAddUserResponse: XyzmoAddUserResponse = newEntity.withResultBase(resultBase)
    xyzmoAddUserResponse.baseResult should be (WebServiceUserAndProfileStub.BaseResultEnum.failed.getValue)
    xyzmoAddUserResponse.error.get should be (WebServiceUserAndProfileStub.ErrorStatus.ArgumentError.getValue)
    xyzmoAddUserResponse.errorMsg.get should be ("omg")
  }

  def newEntity = {
    val celebrity = Celebrity().save()
    new XyzmoAddUserResponse(celebrityId = celebrity.id, baseResult = "ok")
  }

  def saveEntity(toSave: XyzmoAddUserResponse) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoAddUserResponse) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
