package models

import java.sql.Timestamp
import libs.Time
import org.squeryl.PrimitiveTypeMode._
import db.{KeyedCaseClass, Saves, Schema}

/**
 * Persistent entity representing administrators of our service.
 */
case class Administrator(
  id: Long = 0L,
  accountId: Long = 0L,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = Administrator.unapply(this)
}

object Administrator extends Saves[Administrator] with SavesCreatedUpdated[Administrator] {
  //
  // Saves[Administrator] methods
  //
  override val table = Schema.administrators

  override def defineUpdate(theOld: Administrator, theNew: Administrator) = {
    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Administrator] methods
  //
  override def withCreatedUpdated(toUpdate: Administrator, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
