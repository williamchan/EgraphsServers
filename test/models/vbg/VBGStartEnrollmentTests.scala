package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class VBGStartEnrollmentTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGStartEnrollment]
with CreatedUpdatedEntityTests[VBGStartEnrollment]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGStartEnrollment] methods
  //

  val store = AppConfig.instance[VBGStartEnrollmentStore]

  def newEntity = {
    new VBGStartEnrollment()
  }

  def saveEntity(toSave: VBGStartEnrollment) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGStartEnrollment) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}
