package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.Time
import db.{KeyedCaseClass, Schema, Saves}


case class EnrollmentSample(id: Long = 0,
                            enrollmentBatchId: Long,
                            voiceSampleId: Long,
                            signatureSampleId: Long,
                            created: Timestamp = Time.defaultTimestamp,
                            updated: Timestamp = Time.defaultTimestamp)
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): EnrollmentSample = {
    EnrollmentSample.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EnrollmentSample.unapply(this)

}

object EnrollmentSample extends Saves[EnrollmentSample] with SavesCreatedUpdated[EnrollmentSample] {

  //
  // Saves[SignatureEnrollmentAttempt] methods
  //
  override val table = Schema.enrollmentSamples

  override def defineUpdate(theOld: EnrollmentSample, theNew: EnrollmentSample) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.voiceSampleId := theNew.voiceSampleId,
      theOld.signatureSampleId := theNew.signatureSampleId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[EnrollmentSample] methods
  //
  override def withCreatedUpdated(toUpdate: EnrollmentSample, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}