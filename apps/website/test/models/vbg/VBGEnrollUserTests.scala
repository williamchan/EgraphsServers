package models.vbg

import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VBGEnrollUserTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[VBGEnrollUser]
  with CreatedUpdatedEntityTests[Long, VBGEnrollUser]
  with DateShouldMatchers
  with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGEnrollUser] methods
  //

  val store = AppConfig.instance[VBGEnrollUserStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGEnrollUser(errorCode = "50500")
    vbgBase.getErrorCode should be(vbgBase.errorCode)
  }

  "findByEgraph" should "return VBGVerifySample" in {
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
    store.findByEnrollmentBatch(enrollmentBatch) should be(None)

    val vbgVerifySample = new VBGEnrollUser(enrollmentBatchId = enrollmentBatch.id).save()
    store.findByEnrollmentBatch(enrollmentBatch).get should be(vbgVerifySample)
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
    new VBGEnrollUser(enrollmentBatchId = enrollmentBatch.id)
  }

  def saveEntity(toSave: VBGEnrollUser) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: VBGEnrollUser) = {
    toTransform.copy(
      errorCode = "50000"
    )
  }

}