package jobs

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter}
import models._


class EnrollmentBatchJobTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {

//  it should "set EnrollmentBatch.isSuccessfulEnrollment to true if signature and voice samples successfully enroll" in {
//    val celebrity = Celebrity().save()
//    val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
//    enrollmentBatch.addEnrollmentSample(signatureStr = "", voiceStr = "")
//    enrollmentBatch.addEnrollmentSample(signatureStr = "", voiceStr = "")
//    enrollmentBatch.addEnrollmentSample(signatureStr = "", voiceStr = "")
//    enrollmentBatch.addEnrollmentSample(signatureStr = "", voiceStr = "")
//    enrollmentBatch.addEnrollmentSample(signatureStr = "", voiceStr = "")
//    enrollmentBatch.addEnrollmentSample(signatureStr = "", voiceStr = "")
//    enrollmentBatch.copy(isBatchComplete = true).save()
//
//    new EnrollmentBatchJob().now()
//    EnrollmentBatch.findById(enrollmentBatch.id).get.isSuccessfulEnrollment should be(true)
//  }

  "findEnrollmentBatchesPending" should "return batches with true isBatchComplete and null isSuccessfulEnrollment" in {
    for (i <- 0 until EnrollmentBatch.batchSize) EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true, isSuccessfulEnrollment = None).save()

    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true, isSuccessfulEnrollment = Some(false)).save()
    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true, isSuccessfulEnrollment = Some(true)).save()
    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = false, isSuccessfulEnrollment = Some(false)).save()
    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = false, isSuccessfulEnrollment = Some(true)).save()

    val pendingEnrollmentBatches = EnrollmentBatchJob.findEnrollmentBatchesPending()
    pendingEnrollmentBatches.length should be(EnrollmentBatch.batchSize)
  }
}