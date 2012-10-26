package services.http

import play.api.mvc.Request
import services.crypto.Crypto
import services.Time

/**
 * Provides some useful derived information about a request.
 *
 * @param request the Request from which to derive the information
 */
class RequestInfo(request: Request[_]) {
  /** Unique-ish identifier of the client machine; basically a few letters of the hash of the client's IP address */
  lazy val clientId: String = {
    val ipAddress = request.remoteAddress
    val md5 = Crypto.MD5.hash(ipAddress)

    md5.substring(0, 6)
  }

  /**
   * Unique-ish identifier of the request; hashes information about when the requst came in,
   * its URL, and the originating machine and returns the first few characters
   **/
  lazy val requestId: String = {
    val stringToHash = new StringBuilder(request.remoteAddress)
      .append(request.uri)
      .append(Time.now)
    
    val md5 = Crypto.MD5.hash(stringToHash.toString)

    md5.substring(0, 6)
  }
}
