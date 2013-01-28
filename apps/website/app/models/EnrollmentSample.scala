package models

import com.google.inject.Inject
import java.sql.Timestamp
import org.apache.commons.codec.binary.Base64.decodeBase64
import services.AppConfig
import services.blobs.Blobs
import services.db.{KeyedCaseClass, Schema, SavesWithLongKey}
import services.Time
import Blobs.Conversions._

/**
 * Services used by each EnrollmentSample instance
 */
case class EnrollmentSampleServices @Inject()(store: EnrollmentSampleStore, blobs: Blobs)

case class EnrollmentSample(id: Long = 0,
                            enrollmentBatchId: Long = 0,
                            created: Timestamp = Time.defaultTimestamp,
                            updated: Timestamp = Time.defaultTimestamp,
                            services: EnrollmentSampleServices = AppConfig.instance[EnrollmentSampleServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): EnrollmentSample = {
    services.store.save(this)
  }

  def save(signatureStr: String, voiceStr: String): EnrollmentSample = {
    val saved = services.store.save(this)
    services.blobs.put(EnrollmentSample.getSignatureJsonUrl(saved.id), signatureStr.getBytes)
    services.blobs.put(EnrollmentSample.getWavUrl(saved.id), decodeBase64(voiceStr))
    saved
  }

  def getWav: Array[Byte] = {
    services.blobs.get(EnrollmentSample.getWavUrl(id)) match {
      case None => new Array[Byte](0)
      case Some(blob) => blob.asByteArray
    }
  }

  def getSignatureJson: String = {
    services.blobs.get(EnrollmentSample.getSignatureJsonUrl(id)) match {
      case None => ""
      case Some(blob) => blob.asString
    }
  }

  def putSignatureXml(xyzmoSignatureDataContainer: String) {
    services.blobs.put(EnrollmentSample.getSignatureXmlUrl(id), xyzmoSignatureDataContainer.getBytes)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EnrollmentSample.unapply(this)

}

object EnrollmentSample {

  def getSignatureJsonUrl(id: Long): String = {
    "enrollmentsamples/" + id + "/signature.json"
  }

  def getSignatureXmlUrl(id: Long): String = {
    "enrollmentsamples/" + id + "/signature.xml"
  }

  def getWavUrl(id: Long): String = {
    "enrollmentsamples/" + id + "/audio.wav"
  }
}

class EnrollmentSampleStore @Inject()(schema: Schema) extends SavesWithLongKey[EnrollmentSample] with SavesCreatedUpdated[EnrollmentSample] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[EnrollmentSample] methods
  //
  override val table = schema.enrollmentSamples



  //
  // SavesCreatedUpdated[EnrollmentSample] methods
  //
  override def withCreatedUpdated(toUpdate: EnrollmentSample, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}