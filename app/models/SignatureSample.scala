package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import libs.{Blobs, Time}
import Blobs.Conversions._
import db.{KeyedCaseClass, Schema, Saves}
import services.signature.XyzmoBiometricServices
import com.google.inject.Inject
import services.AppConfig

/**
 * Services used in all signature sample instances
 */
case class SignatureSampleServices @Inject() (store: SignatureSampleStore, blobs: Blobs)

case class SignatureSample(
  id: Long = 0,
  isForEnrollment: Boolean = false,
  egraphId: Option[Long] = None,
  signatureEnrollmentAttemptId: Option[Long] = None,
  isRejectedByXyzmoEnrollment: Option[Boolean] = None,
  rejectedByXyzmoEnrollmentReason: Option[String] = None,
  xyzmoVerifyResult: Option[String] = None,
  xyzmoVerifyScore: Option[Int] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: SignatureSampleServices = AppConfig.instance[SignatureSampleServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(signatureStr: String): SignatureSample = {
    val saved = services.store.save(this)
    services.blobs.put(SignatureSample.getJsonUrl(saved.id), signatureStr.getBytes)
    saved
  }

  def putXyzmoSignatureDataContainerOnBlobstore = {
    val jsonStr: String = Blobs.get(SignatureSample.getJsonUrl(id)).get.asString
    val sdc = XyzmoBiometricServices.getSignatureDataContainerFromJSON(jsonStr).getGetSignatureDataContainerFromJSONResult
    services.blobs.put(SignatureSample.getXmlUrl(id), sdc.getBytes)
    sdc
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = SignatureSample.unapply(this)
}

object SignatureSample {

  def getJsonUrl(id: Long): String = {
    "signaturesamples/" + id + ".json"
  }

  def getXmlUrl(id: Long): String = {
    "signaturesamples/" + id + ".xml"
  }
}

class SignatureSampleStore @Inject() (schema: db.Schema) extends Saves[SignatureSample] with SavesCreatedUpdated[SignatureSample] {
  //
  // Saves[SignatureSample] methods
  //
  override val table = schema.signatureSamples

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