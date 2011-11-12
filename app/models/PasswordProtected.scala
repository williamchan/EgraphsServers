package models

import play.db.jpa.Model

import play.data.validation.Validation
import play.data.validation.Validation.ValidationResult


/**
 * Password-protects a JPA Model with SHA256-hashed passwords.
 *
 * Use by mixing into the Model object you want protected.
 *
 * Implementation heavily based on the
 * <a href="https://www.owasp.org/index.php/Hashing_Java">OWASP guidelines</a>
 */
trait PasswordProtected { this: Model =>
  //
  // Instance variables
  //
  /**
   * Base64-encoded SHA256 hash of the password.
   */
  private var _passwordHash: String = null

  /**
   * Base64-encoded 256-bit salt used to secure against rainbow table attacks.
   */
  private var _passwordSalt: String = null

  //
  // Public API
  //

  /**
   * Return the password, which may or may not have been set.
   */
  def password: Option[Password] = {
    (_passwordHash, _passwordSalt) match {
      case (null, null) => None
      case _ => Some(Password(_passwordHash, _passwordSalt))
    }
  }

  /**
   * Sets a new password.
   *
   * The password must be at least 4 characters long.
   *
   * @param newPassword the new password to set
   */
  def setPassword (newPassword: String): ValidationResult = {
    // Perform checks
    val existsCheck = Validation.required("password", newPassword)
    if (existsCheck.ok) {
      val lengthCheck = Validation.minSize("password", newPassword, 4)
      if (lengthCheck.ok) {
        val password = Password(newPassword, this.getId())
        _passwordHash = password.hash
        _passwordSalt = password.salt
      }
      lengthCheck
    }
    else {
      existsCheck
    }
  }
}

object PasswordProtected {

}
