package models

import javax.persistence.Entity
import play.db.jpa.QueryOn

@Entity
class Customer extends User

object Customer extends QueryOn[Customer] with UserQueryOn[Customer]
