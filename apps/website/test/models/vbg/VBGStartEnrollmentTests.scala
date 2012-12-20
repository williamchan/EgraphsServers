package models.vbg

import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}

class VBGStartEnrollmentTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[VBGStartEnrollment]
  with CreatedUpdatedEntityTests[Long, VBGStartEnrollment]
  with DateShouldMatchers
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGStartEnrollment] methods
  //

  def store = AppConfig.instance[VBGStartEnrollmentStore]

  "getErrorCode" should "return errorCode" in new EgraphsTestApplication {
    val vbgBase = new VBGStartEnrollment(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
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
