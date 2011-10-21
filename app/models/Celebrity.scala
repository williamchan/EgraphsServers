package models

import java.util.Date
import javax.persistence.Entity
import play.data.validation.Required
import play.db.jpa.Model

@Entity
class Celebrity extends Model {

  @Required
  var created: Date = null

  @Required
  var updated: Date = null

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

object Celebrity {
  val me = new Celebrity()
}