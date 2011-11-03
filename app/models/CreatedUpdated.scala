package models

import play.db.jpa.Model
import play.data.validation.Required
import java.util.Date
import javax.persistence.{PreUpdate, PrePersist, Temporal, TemporalType}
import org.squeryl.KeyedEntity
import db.{Saves}
import java.sql.Timestamp

trait HasCreatedUpdated {
  def created: Timestamp
  def updated: Timestamp
}

/**
 * Mix into an object that you want to handle saving of objects with created and updated fields
 * (usually this will be the companion object of some case class)
 *
 * Example: {@link Account}
 */
trait SavesCreatedUpdated[T <: KeyedEntity[Long] with HasCreatedUpdated] { this: Saves[T] =>

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


// Old JPA stuff
/**
 * Provides mutable Created and Updated fields for any subclass of
 * Model.
 *
 * You don't have to do anything; just mix in the trait.
 */
trait CreatedUpdated { this: Model =>
  //
  // Public API
  //
  /** The moment of time at which the Model was created */
  @Required
  @Temporal(TemporalType.TIMESTAMP)
  var created: Date = null

  /** The moment of time at which the Model was last updated. */
  @Required
  @Temporal(TemporalType.TIMESTAMP)
  var updated: Date = null

  
  //
  // Private
  //
  @PrePersist
  protected def setCreated() {
    created = new Date
    updated = created
  }

  @PreUpdate
  protected def setUpdated() {
    updated = new Date
  }
}
