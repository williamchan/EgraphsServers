package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each VBGStartVerification instance
 */
case class VBGStartVerificationServices @Inject()(store: VBGStartVerificationStore)

case class VBGStartVerification(id: Long = 0,
                                egraphId: Long = 0,
                                errorCode: String = "",
                                vbgTransactionId: Option[String] = None,
                                created: Timestamp = Time.defaultTimestamp,
                                updated: Timestamp = Time.defaultTimestamp,
                                services: VBGStartVerificationServices = AppConfig.instance[VBGStartVerificationServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGStartVerification = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGStartVerification.unapply(this)

}

class VBGStartVerificationStore @Inject()(schema: Schema) extends Saves[VBGStartVerification] with SavesCreatedUpdated[VBGStartVerification] {

  //
  // Saves[VBGStartVerification] methods
  //
  override val table = schema.vbgStartVerificationTable

  override def defineUpdate(theOld: VBGStartVerification, theNew: VBGStartVerification) = {
    updateIs(
      theOld.egraphId := theNew.egraphId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGStartVerification] methods
  //
  override def withCreatedUpdated(toUpdate: VBGStartVerification, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


