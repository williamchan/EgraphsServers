package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class VBGFinishEnrollTransactionTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGFinishEnrollTransaction]
with CreatedUpdatedEntityTests[VBGFinishEnrollTransaction]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGFinishEnrollTransaction] methods
  //

  val store = AppConfig.instance[VBGFinishEnrollTransactionStore]

  def newEntity = {
    new VBGFinishEnrollTransaction()
  }

  def saveEntity(toSave: VBGFinishEnrollTransaction) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGFinishEnrollTransaction) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}
