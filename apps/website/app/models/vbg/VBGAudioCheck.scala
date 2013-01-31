package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{Schema, SavesWithLongKey}
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
  extends VBGBase
//  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGAudioCheck = {
    services.store.save(this)
  }

  def getErrorCode: String = errorCode

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGAudioCheck.unapply(this)

}

class VBGAudioCheckStore @Inject()(schema: Schema) extends SavesWithLongKey[VBGAudioCheck] with SavesCreatedUpdated[VBGAudioCheck] {

  //
  // SavesWithLongKey[VBGAudioCheck] methods
  //
  override val table = schema.vbgAudioCheckTable



  //
  // SavesCreatedUpdated[VBGAudioCheck] methods
  //
  override def withCreatedUpdated(toUpdate: VBGAudioCheck, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


