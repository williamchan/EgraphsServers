package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.{Blobs, Time}
import db.{KeyedCaseClass, Schema, Saves}

case class VoiceSample(id: Long = 0,
                       isForEnrollment: Boolean,
                       egraphId: Option[Long] = None,
                       voiceEnrollmentAttemptId: Option[Long] = None,
                       vbgAudioCheckErrorCode: Option[String] = None,
                       vbgUsableTime: Option[Int] = None,
                       vbgVerifySampleErrorCode: Option[String] = None,
                       vbgVerifySampleSuccess: Option[String] = None, // turn this into an enum
                       created: Timestamp = Time.defaultTimestamp,
                       updated: Timestamp = Time.defaultTimestamp)
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(voiceStr: String): VoiceSample = {
    val saved = VoiceSample.save(this)
    Blobs.put(VoiceSample.getWavUrl(saved.id), voiceStr.getBytes)
    saved
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VoiceSample.unapply(this)

}

object VoiceSample extends Saves[VoiceSample] with SavesCreatedUpdated[VoiceSample] {

  def getWavUrl(id: Long): String = {
    "voicesamples/" + id + ""
  }

  //
  // Saves[VoiceSample] methods
  //
  override val table = Schema.voiceSamples

  override def defineUpdate(theOld: VoiceSample, theNew: VoiceSample) = {
    updateIs(
      theOld.isForEnrollment := theNew.isForEnrollment,
      theOld.egraphId := theNew.egraphId,
      theOld.voiceEnrollmentAttemptId := theNew.voiceEnrollmentAttemptId,
      theOld.vbgAudioCheckErrorCode := theNew.vbgAudioCheckErrorCode,
      theOld.vbgUsableTime := theNew.vbgUsableTime,
      theOld.vbgVerifySampleErrorCode := theNew.vbgVerifySampleErrorCode,
      theOld.vbgVerifySampleSuccess := theNew.vbgVerifySampleSuccess,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VoiceSample] methods
  //
  override def withCreatedUpdated(toUpdate: VoiceSample, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}