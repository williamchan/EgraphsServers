package models.vbg

import com.google.inject.Inject
import java.sql.Timestamp
import models._
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.{Schema, SavesWithLongKey}
import services.Time

/**
 * Services used by each VBGFinishVerifyTransaction instance
 */
case class VBGFinishVerifyTransactionServices @Inject()(store: VBGFinishVerifyTransactionStore)

case class VBGFinishVerifyTransaction(id: Long = 0,
                                      egraphId: Long = 0,
                                      errorCode: String = "",
                                      vbgTransactionId: Long = 0,
                                      created: Timestamp = Time.defaultTimestamp,
                                      updated: Timestamp = Time.defaultTimestamp,
                                      services: VBGFinishVerifyTransactionServices = AppConfig.instance[VBGFinishVerifyTransactionServices])
  extends VBGBase
  //  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): VBGFinishVerifyTransaction = {
    services.store.save(this)
  }

  def getErrorCode: String = errorCode

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VBGFinishVerifyTransaction.unapply(this)

}

class VBGFinishVerifyTransactionStore @Inject()(schema: Schema) extends SavesWithLongKey[VBGFinishVerifyTransaction] with SavesCreatedUpdated[VBGFinishVerifyTransaction] {

  //
  // SavesWithLongKey[VBGFinishVerifyTransaction] methods
  //
  override val table = schema.vbgFinishVerifyTransactionTable

  override def defineUpdate(theOld: VBGFinishVerifyTransaction, theNew: VBGFinishVerifyTransaction) = {
    updateIs(
      theOld.egraphId := theNew.egraphId,
      theOld.errorCode := theNew.errorCode,
      theOld.vbgTransactionId := theNew.vbgTransactionId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[VBGFinishVerifyTransaction] methods
  //
  override def withCreatedUpdated(toUpdate: VBGFinishVerifyTransaction, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}


