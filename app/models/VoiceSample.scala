package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.Time
import db.{KeyedCaseClass, Saves}
import play.libs.Codec
import com.google.inject.Inject
import services.AppConfig
import services.blobs.Blobs

/**
 * Services used in all voice sample instances
 */
case class VoiceSampleServices @Inject() (store: VoiceSampleStore, blobs: Blobs)

case class VoiceSample(
  id: Long = 0,
  isForEnrollment: Boolean = false,
  egraphId: Option[Long] = None,
  voiceEnrollmentAttemptId: Option[Long] = None,
  vbgAudioCheckErrorCode: Option[String] = None,
  vbgUsableTime: Option[Int] = None,
  vbgVerifySampleErrorCode: Option[String] = None,
  vbgVerifySampleSuccess: Option[String] = None, // turn this into an enum
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: VoiceSampleServices = AppConfig.instance[VoiceSampleServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(voiceStr: String): VoiceSample = {
    val saved = services.store.save(this)
    services.blobs.put(VoiceSample.getWavUrl(saved.id), Codec.decodeBASE64(voiceStr))
    saved
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VoiceSample.unapply(this)

}

object VoiceSample {

  def getWavUrl(id: Long): String = {
    "voicesamples/" + id + ".wav"
  }

}

class VoiceSampleStore @Inject() (schema: db.Schema) extends Saves[VoiceSample] with SavesCreatedUpdated[VoiceSample] {
  //
  // Saves[VoiceSample] methods
  //
  override val table = schema.voiceSamples

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