package models

import java.util.Date
import javax.persistence.Entity
import play.data.validation.Required
import play.db.jpa.Model

object Celebrity {
  val me = new Celebrity
}

// TODO(will): Try to make a case class
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


/* // Anorm
import play.db.anorm._
import play.db.anorm.defaults._

case class Celebrity(celebrityId: Pk[Long] = NotAssigned,
                     created: Date = new Date(),
                     updated: Date = new Date(),
                     udid: String = "",
                     name: String,
                     email: String,
                     password: String,
                     description: String = "",
                     profilePic: String = "",
                     settings: Int = 0,
                     twitterHandle: String = "") {
}

object Celebrity extends Magic[Celebrity] {

  def apply(name: String, email: String, password: String, description: String, profilePic: String): Celebrity = {
    new Celebrity(name = name, email = email, password = password, description = description, profilePic = profilePic)
  }
}
*/
