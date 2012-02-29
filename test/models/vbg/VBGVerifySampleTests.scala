package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

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

  def newEntity = {
    new VBGVerifySample()
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
