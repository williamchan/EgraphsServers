package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{Schema, Saves}
import services.Time

/**
 * Services used by each VBGStartEnrollment instance
 */
case class VBGStartEnrollmentServices @Inject()(store: VBGStartEnrollmentStore)

case class VBGStartEnrollment(id: Long = 0,
                              enrollmentBatchId: Long = 0,
                              errorCode: String = "",
                              vbgTransactionId: Option[Long] = None,
                              created: Timestamp = Time.defaultTimestamp,
                              updated: Timestamp = Time.defaultTimestamp,
                              services: VBGStartEnrollmentServices = AppConfig.instance[VBGStartEnrollmentServices])
  extends VBGBase
  //  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGStartEnrollment = {
    services.store.save(this)
  }

  def getErrorCode: String = errorCode

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGStartEnrollment.unapply(this)

}

class VBGStartEnrollmentStore @Inject()(schema: Schema) extends Saves[VBGStartEnrollment] with SavesCreatedUpdated[VBGStartEnrollment] {

  //
  // Saves[VBGStartEnrollment] methods
  //
  override val table = schema.vbgStartEnrollmentTable

  override def defineUpdate(theOld: VBGStartEnrollment, theNew: VBGStartEnrollment) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGStartEnrollment] methods
  //
  override def withCreatedUpdated(toUpdate: VBGStartEnrollment, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


