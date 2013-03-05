package services.http

import play.api.mvc.Request
import services.crypto.Crypto
import services.Time
import EgraphsSession.Conversions._

/**
 * Provides some useful derived information about a request.
 *
 * @param request the Request from which to derive the information
 */
class RequestInfo(request: Request[_]) {
  /** Unique-ish identifier of the client machine; basically a few letters of the hash of the client's session ID */
  lazy val clientId: String = {
    request.session.id.map { sessionId =>
      Crypto.MD5.hash(sessionId).substring(0, 6)
    }.getOrElse {
      "(Client ID unavailable)"
    }
  }

  /**
   * Unique-ish identifier of the request; hashes information about when the request came in,
   * its URL, and the originating machine and returns the first few characters
   **/
  lazy val requestId: String = {
    val stringToHash = new StringBuilder(clientId)
      .append(request.uri)
      .append(Time.now)
    
    Crypto.MD5.hash(stringToHash.toString).substring(0, 6)
  }
}
