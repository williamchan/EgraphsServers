package models

import javax.persistence.Entity
import play.data.validation.Required
import play.db.jpa.{QueryOn, Model}

@Entity
class Celebrity extends Model with CreatedUpdated {
  @Required
  var name: String = null

  @Required
  var email: String = null

  // TODO(will): store salted and hashed passwords instead
  @Required
  var password: String = null

  var udid: String = null
  var description: String = null
  var profilePic: String = null
  var settings: Int = 0
  var twitterHandle: String = ""

  override def toString = name
}

object Celebrity extends QueryOn[Celebrity]{
  val me = new Celebrity()
}