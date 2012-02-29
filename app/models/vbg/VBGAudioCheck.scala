package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each VBGAudioCheck instance
 */
case class VBGAudioCheckServices @Inject()(store: VBGAudioCheckStore)

case class VBGAudioCheck(id: Long = 0,
                         enrollmentBatchId: Long = 0,
                         errorCode: String = "",
                         vbgTransactionId: Long = 0,
                         usableTime: Option[Double] = None,
                         created: Timestamp = Time.defaultTimestamp,
                         updated: Timestamp = Time.defaultTimestamp,
                         services: VBGAudioCheckServices = AppConfig.instance[VBGAudioCheckServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGAudioCheck = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGAudioCheck.unapply(this)

}

class VBGAudioCheckStore @Inject()(schema: Schema) extends Saves[VBGAudioCheck] with SavesCreatedUpdated[VBGAudioCheck] {

  //
  // Saves[VBGAudioCheck] methods
  //
  override val table = schema.vbgAudioCheckTable

  override def defineUpdate(theOld: VBGAudioCheck, theNew: VBGAudioCheck) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.usableTime := theNew.usableTime,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGAudioCheck] methods
  //
  override def withCreatedUpdated(toUpdate: VBGAudioCheck, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


