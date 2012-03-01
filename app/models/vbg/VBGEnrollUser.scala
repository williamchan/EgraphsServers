package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each VBGEnrollUser instance
 */
case class VBGEnrollUserServices @Inject()(store: VBGEnrollUserStore)

case class VBGEnrollUser(id: Long = 0,
                         enrollmentBatchId: Long = 0,
                         errorCode: String = "",
                         vbgTransactionId: Long = 0,
                         success: Option[Boolean] = None,
                         created: Timestamp = Time.defaultTimestamp,
                         updated: Timestamp = Time.defaultTimestamp,
                         services: VBGEnrollUserServices = AppConfig.instance[VBGEnrollUserServices])
  extends VBGBase
  //  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGEnrollUser = {
    services.store.save(this)
  }

  def getErrorCode: String = errorCode

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGEnrollUser.unapply(this)

}

class VBGEnrollUserStore @Inject()(schema: Schema) extends Saves[VBGEnrollUser] with SavesCreatedUpdated[VBGEnrollUser] {

  //
  // Saves[VBGEnrollUser] methods
  //
  override val table = schema.vbgEnrollUserTable

  override def defineUpdate(theOld: VBGEnrollUser, theNew: VBGEnrollUser) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.success := theNew.success,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGEnrollUser] methods
  //
  override def withCreatedUpdated(toUpdate: VBGEnrollUser, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


