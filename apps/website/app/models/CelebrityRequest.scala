package models

import com.google.inject.Inject
import java.sql.Timestamp
import models.enums.{CelebrityRequestStatus, HasCelebrityRequestStatus}
import services.{ AppConfig, Time }
import services.db.KeyedCaseClass
import services.db.SavesWithLongKey
import services.db.Schema
import services.blobs.Blobs
import services.Time.IntsToSeconds.intsToSecondDurations

case class CelebrityRequestServices @Inject() (store: CelebrityRequestStore)

case class CelebrityRequest(
  id: Long = 0,
  celebrityName: String = "",
  customerId: Long,
  _celebrityRequestStatus: String = CelebrityRequestStatus.PendingAdminReview.name,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CelebrityRequestServices = AppConfig.instance[CelebrityRequestServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasCelebrityRequestStatus[CelebrityRequest] {

  //
  // Public members
  //
  def save(): CelebrityRequest = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = CelebrityRequest.unapply(this)

  //
  // CelebrityRequestStatus[CelebrityRequest] methods
  //
  override def withCelebrityRequestStatus(status: CelebrityRequestStatus.EnumVal) = {
    this.copy(_celebrityRequestStatus = status.name)
  }
}

class CelebrityRequestStore @Inject() (schema: Schema)
  extends SavesWithLongKey[CelebrityRequest] with SavesCreatedUpdated[CelebrityRequest] {

  import org.squeryl.PrimitiveTypeMode._

  def getCelebrityRequestsWithStatus(status: CelebrityRequestStatus.EnumVal): List[CelebrityRequest] = {

    val queryResult = from(schema.celebrityRequests)(celebrityRequest =>
      where(celebrityRequest._celebrityRequestStatus === status.name)
        select (celebrityRequest))

    queryResult.toList
  }

  def getAll: Iterable[CelebrityRequest] = {
    for (celebrityRequest <- schema.celebrityRequests) yield celebrityRequest
  }

  //
  // SavesWithLongKey[CelebrityRequest] methods
  //
  override val table = schema.celebrityRequests

  //
  // SavedCreatedUpdated[CelebrityRequest] methods
  //
  override def withCreatedUpdated(toUpdate: CelebrityRequest, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}