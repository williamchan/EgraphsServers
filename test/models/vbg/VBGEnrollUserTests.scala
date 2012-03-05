package models.vbg

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import models.{EnrollmentBatch, Celebrity}

class VBGEnrollUserTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[VBGEnrollUser]
with CreatedUpdatedEntityTests[VBGEnrollUser]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[VBGEnrollUser] methods
  //

  val store = AppConfig.instance[VBGEnrollUserStore]

  "getErrorCode" should "return errorCode" in {
    val vbgBase = new VBGEnrollUser(errorCode = "50500")
    vbgBase.getErrorCode should be (vbgBase.errorCode)
  }

  def newEntity = {
    val enrollmentBatch = EnrollmentBatch(celebrityId = new Celebrity().save().id).save()
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
