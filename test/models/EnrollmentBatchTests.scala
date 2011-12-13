package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._

class EnrollmentBatchTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[EnrollmentBatch]
with CreatedUpdatedEntityTests[EnrollmentBatch]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[EnrollmentBatch] methods
  //
  def newEntity = {
    val celebrity = Celebrity().save()
    EnrollmentBatch(celebrityId = celebrity.id)
  }

  def saveEntity(toSave: EnrollmentBatch) = {
    EnrollmentBatch.save(toSave)
  }

  def restoreEntity(id: Long) = {
    EnrollmentBatch.findById(id)
  }

  override def transformEntity(toTransform: EnrollmentBatch) = {
    toTransform.copy(
      isBatchComplete = true
    )
  }

  "addEnrollmentSample" should "add EnrollmentSample" in {
    val enrollmentBatch = EnrollmentBatch(celebrityId = Celebrity().save().id).save()
    enrollmentBatch.getNumEnrollmentSamples() should be(0)

    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    enrollmentBatch.getNumEnrollmentSamples() should be(1)

    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    enrollmentBatch.getNumEnrollmentSamples() should be(2)
  }

  "addEnrollmentSample" should "add complete EnrollmentBatch when # of samples reaches batchSize" in {
    var enrollmentBatch = EnrollmentBatch(celebrityId = Celebrity().save().id).save()
    for (i <- 0 until EnrollmentBatch.batchSize - 1) {
      enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    }
    enrollmentBatch.getNumEnrollmentSamples() should be(EnrollmentBatch.batchSize - 1)
    enrollmentBatch.isBatchComplete should be(false)

    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    enrollmentBatch = EnrollmentBatch.findById(enrollmentBatch.id).get
    enrollmentBatch.getNumEnrollmentSamples() should be(EnrollmentBatch.batchSize)
    enrollmentBatch.isBatchComplete should be(true)
  }

}