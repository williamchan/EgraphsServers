package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{Schema, Saves}
import services.Time

/**
 * Services used by each VBGVerifySample instance
 */
case class VBGVerifySampleServices @Inject()(store: VBGVerifySampleStore)

case class VBGVerifySample(id: Long = 0,
                           egraphId: Long = 0,
                           errorCode: String = "",
                           vbgTransactionId: Long = 0,
                           score: Option[Long] = None,
                           success: Option[Boolean] = None,
                           usableTime: Option[Double] = None,
                           created: Timestamp = Time.defaultTimestamp,
                           updated: Timestamp = Time.defaultTimestamp,
                           services: VBGVerifySampleServices = AppConfig.instance[VBGVerifySampleServices])
  extends VBGBase
  //  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGVerifySample = {
    services.store.save(this)
  }

  def getErrorCode: String = errorCode

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
      theOld.egraphId := theNew.egraphId,
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


