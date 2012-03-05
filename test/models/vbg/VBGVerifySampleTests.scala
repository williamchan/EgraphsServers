package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.{Egraph, EgraphTests}

class VBGVerifySampleTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGVerifySample]
with CreatedUpdatedEntityTests[VBGVerifySample]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGVerifySample] methods
  //

  val store = AppConfig.instance[VBGVerifySampleStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGVerifySample(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val egraph = Egraph(orderId = EgraphTests.persistedOrder.id).save()
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
