package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class VBGAudioCheckTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGAudioCheck]
with CreatedUpdatedEntityTests[VBGAudioCheck]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGAudioCheck] methods
  //

  val store = AppConfig.instance[VBGAudioCheckStore]

  def newEntity = {
    new VBGAudioCheck()
  }

  def saveEntity(toSave: VBGAudioCheck) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGAudioCheck) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}
