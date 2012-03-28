package services.http

import play.mvc.Http.Request
import play.libs.Crypto

/**
 * Provides some useful derived information about a request.
 *
 * @param request the Request from which to derive the information
 */
class RequestInfo(request: Request) {
  /** Unique-ish identifier of the client machine; basically a few letters of the hash of the client's IP address */
  lazy val clientId: String = {
    val ipAddress = request.remoteAddress
    val md5 = Crypto.passwordHash(ipAddress, Crypto.HashType.MD5)

    md5.substring(0, 6)
  }

  /**
   * Unique-ish identifier of the request; hashes information about when the requst came in,
   * its URL, and the originating machine and returns the first few characters
   **/
  lazy val requestId: String = {
    val stringToHash = new StringBuilder(request.remoteAddress)
      .append(request.url)
      .append(request.date.getTime)
    
    val md5 = Crypto.passwordHash(stringToHash.toString, Crypto.HashType.MD5)

    md5.substring(0, 6)
  }
}
