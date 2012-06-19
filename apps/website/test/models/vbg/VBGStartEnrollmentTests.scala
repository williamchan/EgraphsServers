package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}

class VBGStartEnrollmentTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGStartEnrollment]
with CreatedUpdatedEntityTests[VBGStartEnrollment]
with ClearsDatabaseAndValidationBefore
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGStartEnrollment] methods
  //

  val store = AppConfig.instance[VBGStartEnrollmentStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGStartEnrollment(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = new Celebrity().save().id).save()
    new VBGStartEnrollment(enrollmentBatchId = enrollmentBatch.id)
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
