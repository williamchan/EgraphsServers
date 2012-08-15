package models.vbg

import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}

class VBGFinishEnrollTransactionTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityTests[VBGFinishEnrollTransaction]
  with CreatedUpdatedEntityTests[VBGFinishEnrollTransaction]
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
