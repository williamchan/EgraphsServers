package services.http

import play.api.mvc.Request
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import egraphs.playutils.Encodings.Base64

//TODO: PLAY20: move into play-utils is probably appropriate for this file.
// This class was written to supplant lack of basic auth in Play 2.0
object BasicAuth {
  case class Credentials(username: String, password: String) {
    def encoded: String = {
      Base64.encode((username + Credentials.fieldDelimiter + password).getBytes)
    }

    def toHeader: (String, String) = {
      (Credentials.headerName, "Basic" + Credentials.headerDelimiter + encoded) 
    }
  }
  
  object Credentials {
    private val headerName = "Authorization"
    private val headerDelimiter = " "
    private val fieldDelimiter = ":"

    def apply(request: RequestHeader): Option[BasicAuth.Credentials] = {
      for (
        authorizationHeaderValue <- request.headers.get(headerName);
        credentials <- this.fromAuthorizationHeader(authorizationHeaderValue)
      ) yield {
        credentials
      }
    }
    
    private[http] def fromAuthorizationHeader(authorizationString: String): Option[BasicAuth.Credentials] = {    
      for (
        encoded <- authorizationString.split(headerDelimiter).drop(1).headOption;      
        credentials <- new String(Base64.decode(encoded)).split(fieldDelimiter).toList match {
          case username :: password :: Nil => Some(BasicAuth.Credentials(username, password))
          case _ => None
        }
      ) yield {
        credentials
      }
    }
  }
  
}