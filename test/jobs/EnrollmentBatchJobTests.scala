package jobs

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import models.SignatureSample._
import utils.{TestConstants, DBTransactionPerTest, ClearsDatabaseAndValidationAfter}
import models.{VoiceSample, SignatureSample, EnrollmentBatch, Celebrity}


class EnrollmentBatchJobTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {

  it should "test" in {
    new EnrollmentBatchJob().now()
  }

  "findEnrollmentBatchesPending" should "return batches with true isBatchComplete and null isSuccessfullEnrollment" in {
    for (i <- 0 until 10) EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true, isSuccessfullEnrollment = None).save()

    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true, isSuccessfullEnrollment = Some(false)).save()
    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true, isSuccessfullEnrollment = Some(true)).save()
    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = false, isSuccessfullEnrollment = Some(false)).save()
    EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = false, isSuccessfullEnrollment = Some(true)).save()

    val pendingEnrollmentBatches = EnrollmentBatchJob.findEnrollmentBatchesPending()
    pendingEnrollmentBatches.length should be(10)
  }

  "getSignatureSamples" should "return SignatureSamples associated with EnrollmentBatch" in {
    val enrollmentBatch = EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true).save()
    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    val signatureSamples: List[SignatureSample] = EnrollmentBatchJob.getSignatureSamples(enrollmentBatch)
    signatureSamples.length should be (2)
  }

  "getVoiceSamples" should "return VoiceSamples associated with EnrollmentBatch" in {
    val enrollmentBatch = EnrollmentBatch(celebrityId = Celebrity().save().id, isBatchComplete = true).save()
    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.signatureStr, voiceStr = TestConstants.voiceStr)
    val voiceSamples: List[VoiceSample] = EnrollmentBatchJob.getVoiceSamples(enrollmentBatch)
    voiceSamples.length should be (2)
  }

}