package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{KeyedCaseClass, Schema, Saves}
import services.Time

/**
 * Services used by each VBGFinishEnrollTransaction instance
 */
case class VBGFinishEnrollTransactionServices @Inject()(store: VBGFinishEnrollTransactionStore)

case class VBGFinishEnrollTransaction(id: Long = 0,
                                      enrollmentBatchId: Long = 0,
                                      errorCode: String = "",
                                      vbgTransactionId: Long = 0,
                                      created: Timestamp = Time.defaultTimestamp,
                                      updated: Timestamp = Time.defaultTimestamp,
                                      services: VBGFinishEnrollTransactionServices = AppConfig.instance[VBGFinishEnrollTransactionServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGFinishEnrollTransaction = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGFinishEnrollTransaction.unapply(this)

}

class VBGFinishEnrollTransactionStore @Inject()(schema: Schema) extends Saves[VBGFinishEnrollTransaction] with SavesCreatedUpdated[VBGFinishEnrollTransaction] {

  //
  // Saves[VBGFinishEnrollTransaction] methods
  //
  override val table = schema.vbgFinishEnrollTransactionTable

  override def defineUpdate(theOld: VBGFinishEnrollTransaction, theNew: VBGFinishEnrollTransaction) = {
    updateIs(
      theOld.enrollmentBatchId := theNew.enrollmentBatchId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGFinishEnrollTransaction] methods
  //
  override def withCreatedUpdated(toUpdate: VBGFinishEnrollTransaction, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


