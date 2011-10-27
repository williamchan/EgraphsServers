package models

import javax.persistence.Entity
import play.db.jpa.QueryOn

/**
 * Persistent entity representing paying purchasers or recipients of the
 * products offered on our service.
 */
@Entity
class Customer extends User

object Customer extends QueryOn[Customer] with UserQueryOn[Customer]
