package models

import play.db.jpa.QueryOn
import javax.persistence.Entity

/**
 * Persistent entity representing administrators of our service.
 */
@Entity
class Administrator extends User

object Administrator extends QueryOn[Administrator] with UserQueryOn[Administrator]
