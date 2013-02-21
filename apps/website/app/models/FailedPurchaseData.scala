package models

import java.sql.Timestamp
import com.google.inject.Inject
import services._
import db.{Schema, SavesWithLongKey, KeyedCaseClass}

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

class FailedPurchaseDataStore @Inject()(schema: Schema) extends SavesWithLongKey[FailedPurchaseData] with SavesCreatedUpdated[FailedPurchaseData] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Saves methods
  //
  def table = schema.failedPurchaseData

  //
  // SavesCreatedUpdated methods
  //
  override def withCreatedUpdated(toUpdate: FailedPurchaseData, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}