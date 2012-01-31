package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.Time
import services.db.{KeyedCaseClass, Schema, Saves}
import com.google.inject.Inject
import services.AppConfig

/**
 * Services used by each EnrollmentSample instance
 */
case class EnrollmentSampleServices @Inject() (store: EnrollmentSampleStore)

case class EnrollmentSample(
  id: Long = 0,
  enrollmentBatchId: Long = 0,
  voiceSampleId: Long = 0,
  signatureSampleId: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: EnrollmentSampleServices = AppConfig.instance[EnrollmentSampleServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): EnrollmentSample = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EnrollmentSample.unapply(this)

}

class EnrollmentSampleStore @Inject() (schema: Schema) extends Saves[EnrollmentSample] with SavesCreatedUpdated[EnrollmentSample] {

  //
  // Saves[SignatureEnrollmentAttempt] methods
  //
  override val table = schema.enrollmentSamples

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