package services.http

import play.api.mvc.Request
import play.api.mvc.Headers
import org.apache.commons.codec.binary.Base64.decodeBase64
import play.api.mvc.RequestHeader

// TODO: PLAY20 migration test this class which we wrote to supplant lack of basic auth in Play 2.0
object BasicAuth {
  case class Credentials(username: String, password: String)
  
  object Credentials {
    def apply(request: RequestHeader): Option[BasicAuth.Credentials] = {
      for (
        authorizationHeaderValue <- request.headers.get("Authorization");
        credentials <- this.fromAuthorizationHeader(authorizationHeaderValue)
      ) yield {
        credentials
      }
    }
    
    private[http] def fromAuthorizationHeader(authorizationString: String): Option[BasicAuth.Credentials] = {    
      for (
        encoded <- authorizationString.split(" ").drop(1).headOption;      
        credentials <- new String(decodeBase64(encoded.getBytes)).split(":").toList match {
          case username :: password :: Nil => Some(BasicAuth.Credentials(username, password))
          case _ => None
        }
      ) yield {
        credentials
      }
    }
  }
  
}