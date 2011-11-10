package models

import java.util.Date
import java.sql.Timestamp
import db.{KeyedCaseClass, Saves}
import libs.Time

/**
 * Mix in with any class that has a created or updated field if you wish its
 * companion object to manage those fields behavior for you using the
 * [[models.SavesCreatedUpdated]] trait.
 *
 * See [[models.Account]] for an example.
 */
trait HasCreatedUpdated {
  def created: Timestamp
  def updated: Timestamp

  /** Renders the created and updated fields as a Map for use in the API */
  def renderCreatedUpdatedForApi: Map[String, Any] = {
    Map(
      "created" -> Time.toApiFormat(created),
      "updated" -> Time.toApiFormat(updated)
    )
  }
}

/**
 * Mix into an object that you want to handle saving of objects with created and updated fields
 * (usually this will be the companion object of some case class)
 *
 * See [[models.Account]] for an example.
 */
trait SavesCreatedUpdated[T <: KeyedCaseClass[Long] with HasCreatedUpdated] { this: Saves[T] =>

  //
  // Abstract members
  //
  /**
   * Provides a new version of a provided model that has correct created and updated fields
   */
  protected def withCreatedUpdated(toUpdate: T, created: Timestamp, updated: Timestamp): T

  //
  // Private implementation
  //
  private def setCreatedAndUpdatedFields(toUpdate: T): T = {
    val timestamp = now
    withCreatedUpdated(toUpdate, timestamp, timestamp)
  }

  private def setUpdatedField(toUpdate: T): T = {
    withCreatedUpdated(toUpdate, created=toUpdate.created, updated=now)
  }

  private def now: Timestamp = {
    new Timestamp(new Date().getTime)
  }

  // Saving lifecycle hooks
  beforeInsert(setCreatedAndUpdatedFields)
  beforeUpdate(setUpdatedField)
}
