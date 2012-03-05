package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}

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

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGFinishEnrollTransaction(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = new Celebrity().save().id).save()
    new VBGFinishEnrollTransaction(enrollmentBatchId = enrollmentBatch.id)
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
