package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.Egraph

class VBGStartVerificationTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGStartVerification]
with CreatedUpdatedEntityTests[VBGStartVerification]
with ClearsDatabaseAndValidationBefore
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGStartVerification] methods
  //

  val store = AppConfig.instance[VBGStartVerificationStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGStartVerification(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val egraph = Egraph(orderId = TestData.newSavedOrder().id).save()
    new VBGStartVerification(egraphId = egraph.id)
  }

  def saveEntity(toSave: VBGStartVerification) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGStartVerification) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}