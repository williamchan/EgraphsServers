package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each VBGVerifySample instance
 */
case class VBGVerifySampleServices @Inject()(store: VBGVerifySampleStore)

case class VBGVerifySample(id: Long = 0,
                           enrollmentBatchId: Long = 0,
                           errorCode: String = "",
                           vbgTransactionId: String = "",
                           score: Option[Long] = None,
                           success: Option[Boolean] = None,
                           usableTime: Option[String] = None,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: VBGVerifySampleServices = AppConfig.instance[VBGVerifySampleServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGVerifySample = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGVerifySample.unapply(this)

}

class VBGVerifySampleStore @Inject()(schema: Schema) extends Saves[VBGVerifySample] with SavesCreatedUpdated[VBGVerifySample] {

  //
  // Saves[VBGVerifySample] methods
  //
  override val table = schema.vbgVerifySampleTable

  override def defineUpdate(theOld: VBGVerifySample, theNew: VBGVerifySample) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.score := theNew.score,
      theOld.success := theNew.success,
      theOld.usableTime := theNew.usableTime,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGVerifySample] methods
  //
  override def withCreatedUpdated(toUpdate: VBGVerifySample, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


