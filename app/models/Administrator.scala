package models

import java.sql.Timestamp
import libs.Time
import org.squeryl.PrimitiveTypeMode._
import db.{KeyedCaseClass, Saves, Schema}
import com.google.inject.Inject

/**
 * Persistent entity representing administrators of our service.
 */
case class Administrator(
  id: Long = 0L,
  // This is currently a meaningless field that exists so that
  // we can pass SavingEntityTests. Get rid of it once we have
  // some Administrator-specific data.
  role: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = Administrator.unapply(this)
}

class AdministratorStore @Inject() (schema: Schema) extends Saves[Administrator] with SavesCreatedUpdated[Administrator] {
  //
  // Saves[Administrator] methods
  //
  override val table = schema.administrators

  override def defineUpdate(theOld: Administrator, theNew: Administrator) = {
    updateIs(
      theOld.created := theNew.created,
      theOld.role := theNew.role,
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
