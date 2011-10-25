package models

import play.db.jpa.Model
import play.data.validation.Required
import java.util.Date
import javax.persistence.{PreUpdate, PrePersist, Temporal, TemporalType}

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
