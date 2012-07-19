package models.vbg

import utils._
import services.AppConfig
import models.Egraph

class VBGVerifySampleTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore
  with SavingEntityTests[VBGVerifySample]
  with CreatedUpdatedEntityTests[VBGVerifySample]
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGVerifySample] methods
  //

  val store = AppConfig.instance[VBGVerifySampleStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGVerifySample(errorCode = "50500")
    vbgBase.getErrorCode should be(vbgBase.errorCode)
  }

  "findByEgraph" should "return VBGVerifySample" in {
    val egraph = Egraph(orderId = TestData.newSavedOrder().id).save()
    store.findByEgraph(egraph) should be(None)

    val vbgVerifySample = new VBGVerifySample(egraphId = egraph.id).save()
    store.findByEgraph(egraph).get should be(vbgVerifySample)
  }

  def newEntity = {
    val egraph = Egraph(orderId = TestData.newSavedOrder().id).save()
    new VBGVerifySample(egraphId = egraph.id)
  }

  def saveEntity(toSave: VBGVerifySample) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGVerifySample) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}
