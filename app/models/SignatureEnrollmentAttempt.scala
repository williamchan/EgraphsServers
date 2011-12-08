package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.Time
import db.{KeyedCaseClass, Schema, Saves}


case class SignatureEnrollmentAttempt(id: Long = 0,
                                      celebrityId: Long,
                                      xyzmoProfileId: String,
                                      xyzmoProfileName: String = "standard",
                                      xyzmoEnrollResult: String = "", // TODO(wchan): Can this be non-null in the database but nullable in memory?
                                      xyzmoContinuous: Boolean = false,
                                      created: Timestamp = Time.defaultTimestamp,
                                      updated: Timestamp = Time.defaultTimestamp)
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): SignatureEnrollmentAttempt = {
    SignatureEnrollmentAttempt.save(this)
  }

  def attempt(): SignatureEnrollmentAttempt = {
    // Find SignatureSamples and call xyzmo server
    this
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = SignatureEnrollmentAttempt.unapply(this)

}

object SignatureEnrollmentAttempt extends Saves[SignatureEnrollmentAttempt] with SavesCreatedUpdated[SignatureEnrollmentAttempt] {

  //
  // Saves[SignatureEnrollmentAttempt] methods
  //
  override val table = Schema.signatureEnrollmentAttempts

  override def defineUpdate(theOld: SignatureEnrollmentAttempt, theNew: SignatureEnrollmentAttempt) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.xyzmoEnrollResult := theNew.xyzmoEnrollResult,
      theOld.xyzmoProfileId := theNew.xyzmoProfileId,
      theOld.xyzmoProfileName := theNew.xyzmoProfileName,
      theOld.xyzmoContinuous := theNew.xyzmoContinuous,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[SignatureEnrollmentAttempt] methods
  //
  override def withCreatedUpdated(toUpdate: SignatureEnrollmentAttempt, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}