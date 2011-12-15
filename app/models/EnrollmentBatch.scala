package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.Time
import db.{KeyedCaseClass, Schema, Saves}


case class EnrollmentBatch(id: Long = 0,
                           celebrityId: Long,
                           isBatchComplete: Boolean = false,
                           isSuccessfulEnrollment: Option[Boolean] = None,
                           // TODO(wchan): Should also store vbg and xyzmo-related metadata
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp)
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): EnrollmentBatch = {
    EnrollmentBatch.save(this)
  }

  def attemptEnrollment(): EnrollmentBatch = {
    // if isBatchComplete, call both xyzmo and vbg
    this
  }

  /**
   * Creates an associated EnrollmentSample. Also updates this.isBatchComplete if the new
   * EnrollmentSample completes this batch (see batchSize).
   */
  def addEnrollmentSample(signatureStr: String, voiceStr: String): (EnrollmentSample, Boolean, Int, Int) = {
    val enrollmentSample = EnrollmentSample(enrollmentBatchId = id,
      signatureSampleId = SignatureSample(isForEnrollment = true).save(signatureStr).id,
      voiceSampleId = VoiceSample(isForEnrollment = true).save(voiceStr).id)
      .save()

    val numEnrollmentSamplesInBatch = getNumEnrollmentSamples()
    if (numEnrollmentSamplesInBatch >= EnrollmentBatch.batchSize) {
      copy(isBatchComplete = true).save()
      // Kick off "job" is EnrollmentBatch is complete
      new jobs.EnrollmentBatchJob().now()
      (enrollmentSample, true, numEnrollmentSamplesInBatch, EnrollmentBatch.batchSize)

    } else {
      (enrollmentSample, false, numEnrollmentSamplesInBatch, EnrollmentBatch.batchSize)
    }
  }

  // TODO(wchan): How to restrict this to just this class and to EnrollmentBatchTests?
  def getNumEnrollmentSamples(): Int = {
    from(Schema.enrollmentSamples)(enrollmentSample =>
      where(enrollmentSample.enrollmentBatchId === id)
        select (enrollmentSample)
    ).size
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EnrollmentBatch.unapply(this)

}

object EnrollmentBatch extends Saves[EnrollmentBatch] with SavesCreatedUpdated[EnrollmentBatch] {

  val batchSize = 10

  //
  // Saves[SignatureEnrollmentAttempt] methods
  //
  override val table = Schema.enrollmentBatches

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