package models.vbg

import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}

class VBGAudioCheckTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityTests[VBGAudioCheck]
  with CreatedUpdatedEntityTests[VBGAudioCheck]
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGAudioCheck] methods
  //

  val store = AppConfig.instance[VBGAudioCheckStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGAudioCheck(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = new Celebrity().save().id).save()
    new VBGAudioCheck(enrollmentBatchId = enrollmentBatch.id)
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
