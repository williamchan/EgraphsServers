package models

import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import db.{Schema, Saves}
import libs.Time
import java.sql.Timestamp


/**
 * Persistent entity representing the Celebrities who provide products on
 * our service.
 */
case class Celebrity(
  id: Long = 0,
  accountId: Long = 0,
  apiKey: Option[String] = Some(""),
  description: Option[String] = Some(""),
  popularName: Option[String] = Some(""),
  profilePhotoId: Option[String] = Some(""),
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedEntity[Long] with HasCreatedUpdated

object Celebrity extends Saves[Celebrity] with SavesCreatedUpdated[Celebrity] {
  //
  // Saves[Celebrity] methods
  //
  override val table = Schema.celebrities

  override def defineUpdate(theOld: Celebrity, theNew: Celebrity) = {
    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.apiKey := theNew.apiKey,
      theOld.description := theNew.description,
      theOld.popularName := theNew.popularName,
      theOld.profilePhotoId := theNew.profilePhotoId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Celebrity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
