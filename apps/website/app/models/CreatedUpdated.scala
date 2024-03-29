package models

import java.util.Date
import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import services.db.{InsertAndUpdateHooks, Saves, KeyedCaseClass, SavesWithLongKey}
import services.Time

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
}

/**
 * Mix into an object that you want to handle saving of objects with created and updated fields
 * (usually this will be the companion object of some case class)
 *
 * See [[models.Account]] for an example.
 */
trait SavesCreatedUpdated[T <: KeyedCaseClass[_] with HasCreatedUpdated] {
  this: InsertAndUpdateHooks[T] =>

  //
  // Abstract members
  //
  /**
   * Provides a new version of a provided model that has correct created and updated fields
   *
   * Note: Every KeyedCaseClass will need to override withCreatedUpdated until SQueryL manual mutation of KeyedEntity.
   */
  protected def withCreatedUpdated(toUpdate: T, created: Timestamp, updated: Timestamp): T

  //
  // Private implementation
  //
  private def setCreatedAndUpdatedFields(toUpdate: T): T = {
    if (toUpdate.created == Time.defaultTimestamp) {
      withCreatedUpdated(toUpdate, now, now)
    } else { // update
      withCreatedUpdated(toUpdate, created = toUpdate.created, updated = now)
    }
  }

  private def now: Timestamp = {
    new Timestamp(new Date().getTime)
  }

  // Saving lifecycle hooks
  beforeInsertOrUpdate(setCreatedAndUpdatedFields)
}
