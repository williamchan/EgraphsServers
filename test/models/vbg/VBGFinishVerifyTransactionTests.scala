package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.{Egraph, EgraphTests}

class VBGFinishVerifyTransactionTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGFinishVerifyTransaction]
with CreatedUpdatedEntityTests[VBGFinishVerifyTransaction]
with ClearsDatabaseAndValidationAfter
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
    val egraph = Egraph(orderId = EgraphTests.persistedOrder.id).save()
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
