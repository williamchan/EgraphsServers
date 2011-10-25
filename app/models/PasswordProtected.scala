package models

import play.data.validation.Required
import play.libs.Crypto
import play.db.jpa.{JPABase, QueryOn, Model}

/**
 * Password-protects a JPA Model. Use by mixing into your Model object.
 */
trait PasswordProtected { this: Model =>
  @Required
  var passwordHash: String = null

  @Required
  var passwordSalt: String = null
  /**
   * Returns true that a provided password is the entity's true password.
   *
   * @param toTest password to test
   *
   */
  def isPassword (toTest: String): Boolean = {
    Crypto.passwordHash(toTest) == passwordHash
  }

  /**
   * Sets a new password
   */
  def setPassword (newPassword: String) {
    // TODO(erem): add salt
    passwordHash = Crypto.passwordHash(newPassword)
  }
}
