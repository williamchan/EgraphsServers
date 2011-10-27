package models

import play.db.jpa.QueryOn
import play.data.validation.Required
import javax.persistence.{OneToMany, Entity}

/**
 * Persistent entity representing the Celebrities who provide products on
 * our service.
 */
@Entity
class Celebrity extends User {
  /** Key used to secure the Celebrity's iPad */
  var apiKey: String = ""

  /** Textual description of the celebrity. */
  @Required
  var description: String = ""

  /** The celebrity's nickname. (e.g. "Shaq" for Shaquille O'Neal) */
  var nickName: String = ""

  /** The celebrity's profile photograph */
  var profilePic: String = null

  /** Products offered by the celebrity */
  @OneToMany
  var products: java.util.List[Product] = new java.util.ArrayList[Product]()

  override def toString = {
    "Celebrity(\"name\")"
  }
}

object Celebrity extends QueryOn[Celebrity] with UserQueryOn[Celebrity] {
  val me = new Celebrity()
}