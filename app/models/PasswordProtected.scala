package models

import play.db.jpa.Model

import libs.Crypto
import java.security.SecureRandom
import java.math.BigInteger
import play.libs.Codec
import annotation.tailrec
import java.util.Date
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
  import PasswordProtected._

  //
  // Instance variables
  //
  /**
   * Base64-encoded SHA256 hash of the password.
   *
   * Performing #saltAndHash on the true password should yield this value.
   */
  var passwordHash: String = null

  /**
   * Base64-encoded 256-bit salt used to secure against rainbow table attacks.
   *
   * Used by #saltAndHash to test attempted passwords.
   */
  var passwordSalt: String = null


  //
  // Public API
  //

  /**
   * Test whether the entity currently has a password set.
   *
   * @return true that a password is set
   */
   def hasPassword = {
     passwordHash != null
   }

  /**
   * Test whether a provided password is the entity's true password.
   *
   * @param toTest the password to test
   * @return true that the provided password is the entity's password.
   */
  def passwordIs (attempt: String): Boolean = {
    saltAndHash(attempt) == passwordHash
  }

  /**
   * Sets a new password.
   *
   * Also generates and sets a new salt onto the entity, not that you should
   * have to care about that.
   *
   * @param newPassword the new password to set
   */
  def setPassword (newPassword: String): ValidationResult = {
    val existsCheck = Validation.required("password", newPassword)
    if (existsCheck.ok) {
      val lengthCheck = Validation.minSize("password", newPassword, 4)
      if (lengthCheck.ok) {
        // Create new salt. Add timestamp to the random number to further ensure
        // uniqueness in our user database.
        passwordSalt = hash(randomSaltNumber() + this.getId + new Date().getTime)

        // Generate hash
        passwordHash = saltAndHash(newPassword)
      }
      lengthCheck
    }
    else {
      existsCheck
    }
  }

  //
  // Private methods
  //
  /**
   * Salts a provided string and hashes it {@link PasswordProtected#timesToHash}
   * times. Only call this once the correct salt has already been set on
   * this object.
   *
   * It seeds the hash loop with the concatenation of the password string
   * and #passwordSalt.
   *
   * @param password the string to hash and salt
   * @return the hashed and salted version of the string. This product would be
   *     suitable to set into the {@link #passwordHash} field.
   */
  private def saltAndHash (password: String): String = {
    hash(password + passwordSalt, times=timesToHash)
  }

  object PasswordProtected {

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
     * Performs the SHA256 hash function n times against a String parameter.
     *
     * @param toHash the string to hash
     * @param times the number of times to hash the string
     *
     * @return result of the hash function iterated n times
     */
    @tailrec
    def hash (toHash: String, times: Int = 1): String = {
      require(times > 0)

      times match {
        case 1 => Crypto.passwordHash(toHash, Crypto.HashType.SHA256)
        case n => hash(toHash, times - 1)
      }
    }
  }
}
