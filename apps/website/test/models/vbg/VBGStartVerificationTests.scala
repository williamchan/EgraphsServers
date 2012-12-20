package models.vbg

import utils._
import services.AppConfig
import models.Egraph

class VBGStartVerificationTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[VBGStartVerification]
  with CreatedUpdatedEntityTests[Long, VBGStartVerification]
  with DateShouldMatchers
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGStartVerification] methods
  //

  def store = AppConfig.instance[VBGStartVerificationStore]

  "getErrorCode" should "return errorCode" in new EgraphsTestApplication {
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
