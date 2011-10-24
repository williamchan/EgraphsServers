package models

import play.db.jpa.Model
import play.data.validation.Required
import java.util.Date

/**
 * Provides mutable Created and Updated fields for any mixed in JPA model class.
 *
 * Mix it in with any JPA Model and call <code>saveAndTimestamp</code> instead of
 * <code>save</code>.
 */
trait CreatedUpdated { this: Model =>

  /** The moment of time at which the Model was created */
  @Required
  var created: Date = null

  /** The moment of time at which the Model was last updated. */
  @Required
  var updated: Date = null

  def saveAndTimestamp: this.type = {
    created = if (created == null) new Date else created
    updated = new Date

    this.save()
  }
}
