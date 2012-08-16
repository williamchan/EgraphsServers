package models.vbg

import utils._
import services.AppConfig
import models.Egraph

class VBGFinishVerifyTransactionTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityTests[VBGFinishVerifyTransaction]
  with CreatedUpdatedEntityTests[VBGFinishVerifyTransaction]
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGFinishVerifyTransaction] methods
  //

  val store = AppConfig.instance[VBGFinishVerifyTransactionStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGFinishVerifyTransaction(errorCode = "50500")
    vbgBase.getErrorCode should be(vbgBase.errorCode)
  }

  def newEntity = {
    val egraph = Egraph(orderId = TestData.newSavedOrder().id).save()
    new VBGFinishVerifyTransaction(egraphId = egraph.id)
  }

  def saveEntity(toSave: VBGFinishVerifyTransaction) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGFinishVerifyTransaction) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}
