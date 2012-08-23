package models

import enums.EnrollmentStatus
import utils._
import services.AppConfig

class EnrollmentBatchTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[EnrollmentBatch]
  with CreatedUpdatedEntityTests[Long, EnrollmentBatch]
  with DBTransactionPerTest {

  val store = AppConfig.instance[EnrollmentBatchStore]
  val celebStore = AppConfig.instance[CelebrityStore]

  //
  // SavingEntityTests[EnrollmentBatch] methods
  //
  def newEntity = {
    val celebrity = TestData.newSavedCelebrity()
    EnrollmentBatch(celebrityId = celebrity.id)
  }

  def saveEntity(toSave: EnrollmentBatch) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: EnrollmentBatch) = {
    toTransform.copy(
      isBatchComplete = true
    )
  }

  "addEnrollmentSample" should "add EnrollmentSample" in {
    val enrollmentBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id).save()
    enrollmentBatch.getNumEnrollmentSamples should be(0)

    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.shortWritingStr, voiceStr = TestConstants.fakeAudioStr())
    enrollmentBatch.getNumEnrollmentSamples should be(1)

    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.shortWritingStr, voiceStr = TestConstants.fakeAudioStr())
    enrollmentBatch.getNumEnrollmentSamples should be(2)
  }

  "addEnrollmentSample" should "add complete EnrollmentBatch when # of samples reaches batchSize" in {
    val celeb = TestData.newSavedCelebrity()
    celeb.enrollmentStatus should be(EnrollmentStatus.NotEnrolled)

    var enrollmentBatch = EnrollmentBatch(celebrityId = celeb.id).save()
    for (i <- 0 until EnrollmentBatch.batchSize - 1) {
      enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.shortWritingStr, voiceStr = TestConstants.fakeAudioStr())
    }
    enrollmentBatch.getNumEnrollmentSamples should be(EnrollmentBatch.batchSize - 1)
    enrollmentBatch.isBatchComplete should be(false)

    enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.shortWritingStr, voiceStr = TestConstants.fakeAudioStr())
    enrollmentBatch = store.get(enrollmentBatch.id)
    enrollmentBatch.getNumEnrollmentSamples should be(EnrollmentBatch.batchSize)
    enrollmentBatch.isBatchComplete should be(true)

    celebStore.get(celeb.id).enrollmentStatus should be(EnrollmentStatus.AttemptingEnrollment)
  }

  "getEnrollmentSamples" should "return list of EnrollmentSamples" in {
    val celeb = TestData.newSavedCelebrity()
    celeb.enrollmentStatus should be(EnrollmentStatus.NotEnrolled)

    val enrollmentBatch = EnrollmentBatch(celebrityId = celeb.id).save()
    enrollmentBatch.getEnrollmentSamples.size should be(0)
    for (i <- 0 until EnrollmentBatch.batchSize - 1) {
      enrollmentBatch.addEnrollmentSample(signatureStr = TestConstants.shortWritingStr, voiceStr = TestConstants.fakeAudioStr())
      enrollmentBatch.getEnrollmentSamples.size should be(i + 1)
    }
  }

  "getEnrollmentBatchesPending" should "return batches with true isBatchComplete and null isSuccessfulEnrollment" in {
    val initialPendingEnrollmentBatchesSize = store.getEnrollmentBatchesPending().size

    val pendingBatch = EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id, isBatchComplete = true, isSuccessfulEnrollment = None).save()
    EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id, isBatchComplete = true, isSuccessfulEnrollment = Some(false)).save()
    EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id, isBatchComplete = true, isSuccessfulEnrollment = Some(true)).save()
    EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id, isBatchComplete = false, isSuccessfulEnrollment = Some(false)).save()
    EnrollmentBatch(celebrityId = TestData.newSavedCelebrity().id, isBatchComplete = false, isSuccessfulEnrollment = Some(true)).save()

    val pendingEnrollmentBatches = store.getEnrollmentBatchesPending()
    pendingEnrollmentBatches.length should be > (initialPendingEnrollmentBatchesSize)
    pendingEnrollmentBatches should contain (pendingBatch)
  }

  "getOpenEnrollmentBatch" should "return the open EnrollmentBatch" in {
    val celebrity = TestData.newSavedCelebrity()
    val batch = EnrollmentBatch(celebrityId = celebrity.id).save()
    val result = store.getOpenEnrollmentBatch(celebrity)
    result.get should be (batch)
  }

}