package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.Time
import services.db.{KeyedCaseClass, Schema, Saves}
import services.AppConfig
import services.signature.{SignatureBiometricService, SignatureBiometricsError}
import services.voice.{VoiceBiometricService, VoiceBiometricsError}
import com.google.inject.{Provider, Inject}
import org.squeryl.Query
import services.blobs.Blobs

/**
 * Services used by each EnrollmentBatch instance.
 */
case class EnrollmentBatchServices @Inject() (
  store: EnrollmentBatchStore,
  celebStore: CelebrityStore,
  enrollmentSampleServices: Provider[EnrollmentSampleServices],
  blobs: Blobs,
  voiceBiometrics: VoiceBiometricService,
  signatureBiometrics: SignatureBiometricService
)

case class EnrollmentBatch(id: Long = 0,
                           celebrityId: Long = 0,
                           isBatchComplete: Boolean = false,
                           isSuccessfulEnrollment: Option[Boolean] = None,
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
    val enrollmentSample = EnrollmentSample(
      enrollmentBatchId=id,
      services=services.enrollmentSampleServices.get
    ).save(signatureStr = signatureStr, voiceStr = voiceStr)

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

  def enrollSignature: Either[SignatureBiometricsError, Boolean] = {
    services.signatureBiometrics.enroll(this)
  }

  def enrollVoice: Either[VoiceBiometricsError, Boolean] = {
    services.voiceBiometrics.enroll(this)
  }

  def getEnrollmentSamples: List[EnrollmentSample] = {
    services.store.getEnrollmentSamples(id)
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
  val batchSize = 20 // This needs to match the number of enrollment phrases in GetCelebrityEnrollmentTemplateApiEndpoint

  def getCombinedWavUrl(id: Long): String = {
    "enrollmentbatches/" + id + "/combined.wav"
  }
}

class EnrollmentBatchStore @Inject() (schema: Schema) extends Saves[EnrollmentBatch] with SavesCreatedUpdated[EnrollmentBatch] {

  def getEnrollmentSamples(batchId: Long): List[EnrollmentSample] = {
    queryForEnrollmentSamples(batchId).toList
  }

  def countEnrollmentSamples(batchId: Long): Int = {
    queryForEnrollmentSamples(batchId).size
  }

  private def queryForEnrollmentSamples(batchId: Long): Query[EnrollmentSample] = {
    from(schema.enrollmentSamples)(enrollmentSample =>
      where(enrollmentSample.enrollmentBatchId === batchId)
        select (enrollmentSample)
    )
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
