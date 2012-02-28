package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.Time
import services.db.{KeyedCaseClass, Schema, Saves}
import services.AppConfig
import com.google.inject.{Provider, Inject}

/**
 * Services used by each EnrollmentBatch instance.
 */
case class EnrollmentBatchServices @Inject() (
  store: EnrollmentBatchStore,
  celebStore: CelebrityStore,
  enrollmentSampleServices: Provider[EnrollmentSampleServices],
  signatureSampleServices: Provider[SignatureSampleServices],
  voiceSampleServices: Provider[VoiceSampleServices]
)

case class EnrollmentBatch(id: Long = 0,
                           celebrityId: Long = 0,
                           isBatchComplete: Boolean = false,
                           isSuccessfulEnrollment: Option[Boolean] = None,
                           // TODO(wchan): Should also store vbg and xyzmo-related metadata
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: EnrollmentBatchServices = AppConfig.instance[EnrollmentBatchServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated
{
  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): EnrollmentBatch = {
    services.store.save(this)
  }

  /**
   * Creates an associated EnrollmentSample. Also updates this.isBatchComplete if the new
   * EnrollmentSample completes this batch (see batchSize).
   */
  def addEnrollmentSample(signatureStr: String, voiceStr: String, skipBiometrics: Boolean = false): (EnrollmentSample, Boolean, Int, Int) = {
    val signatureSample = SignatureSample(
      isForEnrollment=true,
      services=services.signatureSampleServices.get
    ).save(signatureStr)

    val voiceSample = VoiceSample(
      isForEnrollment=true,
      services=services.voiceSampleServices.get
    ).save(voiceStr)

    val enrollmentSample = EnrollmentSample(
      enrollmentBatchId=id,
      signatureSampleId=signatureSample.id,
      voiceSampleId=voiceSample.id,
      services=services.enrollmentSampleServices.get
    ).save()

    val numEnrollmentSamplesInBatch = getNumEnrollmentSamples
    if (numEnrollmentSamplesInBatch >= EnrollmentBatch.batchSize) {

      copy(isBatchComplete = true).save()
      services.celebStore.get(celebrityId).withEnrollmentStatus(EnrollmentStatus.AttemptingEnrollment).save()

      // Kick off "job" is EnrollmentBatch is complete
      if (!skipBiometrics) {
        new jobs.EnrollmentBatchJob().now()
      }

      (enrollmentSample, true, numEnrollmentSamplesInBatch, EnrollmentBatch.batchSize)

    } else {
      (enrollmentSample, false, numEnrollmentSamplesInBatch, EnrollmentBatch.batchSize)
    }
  }

  private[models] def getNumEnrollmentSamples: Int = {
    services.store.countEnrollmentSamples(id)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EnrollmentBatch.unapply(this)

}

object EnrollmentBatch {
  val batchSize = 10
}

class EnrollmentBatchStore @Inject() (schema: Schema) extends Saves[EnrollmentBatch] with SavesCreatedUpdated[EnrollmentBatch] {
  //
  // Public methods
  //
  def countEnrollmentSamples(batchId: Long): Int = {
    from(schema.enrollmentSamples)(enrollmentSample =>
      where(enrollmentSample.enrollmentBatchId === batchId)
        select (enrollmentSample)
    ).size
  }

  //
  // Saves[EnrollmentBatch] methods
  //
  override val table = schema.enrollmentBatches

  override def defineUpdate(theOld: EnrollmentBatch, theNew: EnrollmentBatch) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.isBatchComplete := theNew.isBatchComplete,
      theOld.isSuccessfulEnrollment := theNew.isSuccessfulEnrollment,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[EnrollmentBatch] methods
  //
  override def withCreatedUpdated(toUpdate: EnrollmentBatch, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
