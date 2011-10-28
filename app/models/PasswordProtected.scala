package models

import play.db.jpa.Model

import java.security.SecureRandom
import java.math.BigInteger
import play.libs.Codec
import java.util.Date
import play.data.validation.Validation
import play.data.validation.Validation.ValidationResult

import libs.Crypto

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

case class Password(hash: String, salt: String) {
  /**
   * Test whether a provided password is the true password.
   *
   * @param toTest the password to test
   * @return true that the provided password is the entity's password.
   */
  def is (attempt: String): Boolean = {
    Password.hashPassword(attempt, salt) == this.hash
  }
}

object Password {
  /**
   * The number of times to perform the hash function before arriving at the
   * final password. Should be an arbitrary number above 1000.
   */
  val timesToHash = 1183

  /**
   * Cryptographically secure random number generator. These are expensive to
   * seed, so we keep one around for repeated use.
   */
  val random = SecureRandom.getInstance("SHA1PRNG")

  /** Returns a new, random number sized against our hash function's cipher. */
  def randomSaltNumber (): String = {
    Codec.encodeBASE64(new BigInteger(256, random).toByteArray)
  }

  /**
   * Create a Password with only a password and unique id for the entity being protected.
   *
   * @param password the desired password
   * @param entityId unique id of the entity being protected.
   *
   * @return a Password object whose Password#is method returns true for the correct
   *   password
   */
  def apply(password: String, entityId: Long): Password = {
    val theSalt = hashNTimes("" + randomSaltNumber + entityId + new Date().getTime, times=1)
    Password(hash=hashPassword(password, theSalt), salt=theSalt)
  }

  /**
   * Performs the SHA256 hash function n times against a String parameter.
   *
   * @param toHash the string to hash
   * @param times the number of times to hash the string
   *
   * @return result of the hash function iterated n times
   */
  def hashNTimes (toHash: String, times: Int = 1): String = {
    import Crypto.passwordHash
    import Crypto.HashType.SHA256

    (1 to times).foldLeft(toHash)((nthHash, _) => passwordHash(nthHash, SHA256))
  }

  /**
   * Hashes the password and salt together #timesToHash times.
   */
  def hashPassword(password: String, salt: String): String = {
    hashNTimes(password + salt, times=timesToHash)
  }
}

object PasswordProtected {

}
