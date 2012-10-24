package models

import com.google.inject.Inject
import services._
import db.{Schema, SavesWithLongKey, KeyedCaseClass}
import java.sql.Timestamp

case class FailedPurchaseData(
  id: Long = 0L,
  purchaseData: String = "",
  errorDescription: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: FailedPurchaseDataServices = AppConfig.instance[FailedPurchaseDataServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

  override def unapplied = FailedPurchaseData.unapply(this)

  def save(): FailedPurchaseData = services.store.save(this)
}

case class FailedPurchaseDataServices @Inject()(store: FailedPurchaseDataStore)

class FailedPurchaseDataStore @Inject()(schema: Schema) extends SavesWithLongKey[FailedPurchaseData] with SavesCreatedUpdated[Long,FailedPurchaseData] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Saves methods
  //
  def table = schema.failedPurchaseData

  override def defineUpdate(theOld: FailedPurchaseData, theNew: FailedPurchaseData) = {
    updateIs(
      theOld.purchaseData := theNew.purchaseData,
      theOld.errorDescription := theNew.errorDescription,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated methods
  //
  override def withCreatedUpdated(toUpdate: FailedPurchaseData, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}