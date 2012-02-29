package models.xyzmo

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.Celebrity
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub

class XyzmoDeleteUserTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[XyzmoDeleteUser]
with CreatedUpdatedEntityTests[XyzmoDeleteUser]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[XyzmoDeleteUser] methods
  //

  val store = AppConfig.instance[XyzmoDeleteUserStore]

  "withResultBase" should "populate base fields" in {
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
    val celebrity = Celebrity().save()
    new XyzmoDeleteUser(celebrityId = celebrity.id, baseResult = "ok")
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
