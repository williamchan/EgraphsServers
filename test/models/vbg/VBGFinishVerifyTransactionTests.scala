package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

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

  def newEntity = {
    new VBGFinishVerifyTransaction()
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
