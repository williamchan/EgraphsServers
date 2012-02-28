package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.Celebrity
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub

class XyzmoDeleteUserResponseTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoDeleteUserResponse]
with CreatedUpdatedEntityTests[XyzmoDeleteUserResponse]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoDeleteUserResponse] methods
  //

  val store = AppConfig.instance[XyzmoDeleteUserResponseStore]

  "withResultBase" should "populate base fields" in {
    val resultBase = new WebServiceUserAndProfileStub.ResultBase
    resultBase.setBaseResult(WebServiceUserAndProfileStub.BaseResultEnum.failed)
    val errorInfo = new WebServiceUserAndProfileStub.ErrorInfo
    errorInfo.setError(WebServiceUserAndProfileStub.ErrorStatus.ArgumentError)
    errorInfo.setErrorMsg("omg")
    resultBase.setErrorInfo(errorInfo)

    val xyzmoDeleteUserResponse: XyzmoDeleteUserResponse = newEntity.withResultBase(resultBase)
    xyzmoDeleteUserResponse.baseResult should be (WebServiceUserAndProfileStub.BaseResultEnum.failed.getValue)
    xyzmoDeleteUserResponse.error.get should be (WebServiceUserAndProfileStub.ErrorStatus.ArgumentError.getValue)
    xyzmoDeleteUserResponse.errorMsg.get should be ("omg")
  }

  def newEntity = {
    val celebrity = Celebrity().save()
    new XyzmoDeleteUserResponse(celebrityId = celebrity.id, baseResult = "ok")
  }

  def saveEntity(toSave: XyzmoDeleteUserResponse) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: XyzmoDeleteUserResponse) = {
    toTransform.copy(
      baseResult = "failed"
    )
  }

}
