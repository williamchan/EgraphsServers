package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.Time
import db.{KeyedCaseClass, Schema, Saves}

case class SignatureSample(id: Long = 0,
                           s3Location: String,
                           isForEnrollment: Boolean,
                           egraphId: Option[Long] = None,
                           signatureEnrollmentAttemptId: Option[Long] = None,
                           isRejectedByXyzmoEnrollment: Option[Boolean] = None,
                           rejectedByXyzmoEnrollmentReason: Option[String] = None,
                           xyzmoVerifyResult: Option[String] = None,
                           xyzmoVerifyScore: Option[Int] = None,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp)
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): SignatureSample = {
    SignatureSample.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = SignatureSample.unapply(this)

}

object SignatureSample extends Saves[SignatureSample] with SavesCreatedUpdated[SignatureSample] {

  //
  // Saves[SignatureSample] methods
  //
  override val table = Schema.signatureSamples

  override def defineUpdate(theOld: SignatureSample, theNew: SignatureSample) = {
    updateIs(
      theOld.s3Location := theNew.s3Location,
      theOld.isForEnrollment := theNew.isForEnrollment,
      theOld.egraphId := theNew.egraphId,
      theOld.signatureEnrollmentAttemptId := theNew.signatureEnrollmentAttemptId,
      theOld.isRejectedByXyzmoEnrollment := theNew.isRejectedByXyzmoEnrollment,
      theOld.rejectedByXyzmoEnrollmentReason := theNew.rejectedByXyzmoEnrollmentReason,
      theOld.xyzmoVerifyResult := theNew.xyzmoVerifyResult,
      theOld.xyzmoVerifyScore := theNew.xyzmoVerifyScore,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[SignatureSample] methods
  //
  override def withCreatedUpdated(toUpdate: SignatureSample, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}