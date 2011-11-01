package models

import java.security.SecureRandom
import play.libs.Codec
import libs.Crypto
import java.math.BigInteger
import java.util.Date

case class Password (hash: String, salt: String) {
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
  def hashNTimes(toHash: String, times: Int = 1): String = {
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