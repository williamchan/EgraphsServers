package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.{Blobs, Time}
import db.{KeyedCaseClass, Schema, Saves}

case class SignatureSample(id: Long = 0,
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
  def save(signatureStr: String): SignatureSample = {
    val saved = SignatureSample.save(this)
    Blobs.put(SignatureSample.getJsonUrl(saved.id), signatureStr.getBytes)
    saved
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = SignatureSample.unapply(this)

}

object SignatureSample extends Saves[SignatureSample] with SavesCreatedUpdated[SignatureSample] {

  def getJsonUrl(id: Long): String = {
    "signaturesamples/" + id + ".json"
  }

  def getXmlUrl(id: Long): String = {
    "signaturesamples/" + id + ".xml"
  }

  //
  // Saves[SignatureSample] methods
  //
  override val table = Schema.signatureSamples

  override def defineUpdate(theOld: SignatureSample, theNew: SignatureSample) = {
    updateIs(
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