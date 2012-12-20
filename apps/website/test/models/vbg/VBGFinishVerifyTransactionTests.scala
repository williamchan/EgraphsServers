package models.vbg

import utils._
import services.AppConfig
import models.Egraph

class VBGFinishVerifyTransactionTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[VBGFinishVerifyTransaction]
  with CreatedUpdatedEntityTests[Long, VBGFinishVerifyTransaction]
  with DateShouldMatchers
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGFinishVerifyTransaction] methods
  //

  def store = AppConfig.instance[VBGFinishVerifyTransactionStore]

  "getErrorCode" should "return errorCode" in new EgraphsTestApplication {
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
