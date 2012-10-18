package models.vbg

import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VBGAudioCheckTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[VBGAudioCheck]
  with CreatedUpdatedEntityTests[Long, VBGAudioCheck]
  with DateShouldMatchers
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
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
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