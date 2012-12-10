package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{Schema, SavesWithLongKey}
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
  extends VBGBase
  //  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGFinishEnrollTransaction = {
    services.store.save(this)
  }

  def getErrorCode: String = errorCode

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGFinishEnrollTransaction.unapply(this)

}

class VBGFinishEnrollTransactionStore @Inject()(schema: Schema) extends SavesWithLongKey[VBGFinishEnrollTransaction] with SavesCreatedUpdated[VBGFinishEnrollTransaction] {

  //
  // SavesWithLongKey[VBGFinishEnrollTransaction] methods
  //
  override val table = schema.vbgFinishEnrollTransactionTable



  //
  // SavesCreatedUpdated[VBGFinishEnrollTransaction] methods
  //
  override def withCreatedUpdated(toUpdate: VBGFinishEnrollTransaction, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


