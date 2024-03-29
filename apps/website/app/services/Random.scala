package services

import java.security.SecureRandom
import org.postgresql.util.Base64

/**
 * Source for random data.
 */
object Random {
  val generator = new SecureRandom()

  /**
   * Get a String of Base64-encoded randomness.
   *
   * @param byteCount the number of random bytes to encode and return.
   */
  def string(byteCount: Int): String = {
    val bytes = new Array[Byte](byteCount)
    generator.nextBytes(bytes)

    Base64.encodeBytes(bytes)
  }
}
