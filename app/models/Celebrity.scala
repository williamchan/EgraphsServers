package models

import org.squeryl.PrimitiveTypeMode._
import libs.Time
import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}


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
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  override def unapplied = Celebrity.unapply(this)
}

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
