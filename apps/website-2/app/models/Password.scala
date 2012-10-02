package models

import java.security.SecureRandom
import play.api.libs.Codecs
import java.math.BigInteger
import java.util.Date
import play.data.validation.Validation
import org.postgresql.util.Base64
import services.crypto.Crypto.SHA256

case class Password (hash: String, salt: String) {
  /**
   * Test whether a provided password is the true password.
   *
   * @param attempt the password to test
   * @return true that the provided password is the entity's password.
   */
  def is (attempt: String): Boolean = {
    Password.hashPassword(attempt, salt) == this.hash
  }
}

object Password {
  trait PasswordError {
    def message: String
  }
  
  case object PasswordRequired extends PasswordError {
    override val message = "Non-empty password is required"
  }
  
  case class PasswordTooLong(attempted: String) extends PasswordError {
    override val message = "Password must be at least " + Account.minPasswordLength + "characters"
  } 
  
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
    Base64.encodeBytes(new BigInteger(256, random).toByteArray)    
  }

  /**
   * @param password to validate
   * @return validation errors or the password
   */
  def validate(password: String): Either[PasswordError, String] = {
    if (password != null && password != "") {
      if (password.length() >= Account.minPasswordLength) {
        Right(password)
      } else {
        Left(PasswordTooLong(password))
      }
    } else {
      Left(PasswordRequired)
    }
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
  def hashNTimes(toHash: String, times: Int = 1): String = {    
    
    (1 to times).foldLeft(toHash)((nthHash, _) => SHA256.hash(nthHash))
  }

  /**
   * Hashes the password and salt together #timesToHash times.
   */
  def hashPassword(password: String, salt: String): String = {
    hashNTimes(password + salt, times=timesToHash)
  }
}
