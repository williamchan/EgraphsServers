package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class EnrollmentSampleTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[EnrollmentSample]
with CreatedUpdatedEntityTests[EnrollmentSample]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  //
  // SavingEntityTests[EnrollmentSample] methods
  //

  val store = AppConfig.instance[EnrollmentSampleStore]

  def newEntity = {
    val celebrity = Celebrity().save()
    val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
    val signatureSample = SignatureSample(isForEnrollment = true).save(TestConstants.signatureStr)
    val voiceSample = VoiceSample(isForEnrollment = true).save(TestConstants.voiceStr)
    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id,
      voiceSampleId = voiceSample.id,
      signatureSampleId = signatureSample.id)
  }

  def saveEntity(toSave: EnrollmentSample) = {
    store.save(toSave)
  }

  def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: EnrollmentSample) = {
    val signatureSample = SignatureSample(isForEnrollment = true).save(TestConstants.signatureStr)
    toTransform.copy(
      // Actually, not much to test here. Just providing something here for now.
      signatureSampleId = signatureSample.id
    )
  }

}