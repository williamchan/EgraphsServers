package models

import com.google.inject.Inject
import java.sql.Timestamp
import org.squeryl.PrimitiveTypeMode._
import play.libs.Codec
import services.AppConfig
import services.blobs.Blobs
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time
import org.jclouds.blobstore.domain.Blob
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
    services.blobs.put(EnrollmentSample.getWavUrl(saved.id), Codec.decodeBASE64(voiceStr))
    saved
  }

  def getWav: Array[Byte] = {
    val blob: Option[Blob] = services.blobs.get(EnrollmentSample.getWavUrl(id))
    if (blob.isDefined) {
      blob.get.asByteArray
    } else {
      new Array[Byte](0)
    }
  }

  def getSignatureJson: String = {
    val blob: Option[Blob] = services.blobs.get(EnrollmentSample.getSignatureJsonUrl(id))
    if (blob.isDefined) {
      blob.get.asString
    } else {
      ""
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

class EnrollmentSampleStore @Inject()(schema: Schema) extends Saves[EnrollmentSample] with SavesCreatedUpdated[EnrollmentSample] {

  //
  // Saves[EnrollmentSample] methods
  //
  override val table = schema.enrollmentSamples

  override def defineUpdate(theOld: EnrollmentSample, theNew: EnrollmentSample) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
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