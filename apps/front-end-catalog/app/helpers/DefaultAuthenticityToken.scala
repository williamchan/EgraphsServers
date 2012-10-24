package helpers

import egraphs.authtoken.AuthenticityToken

trait DefaultAuthenticityToken {
  implicit val authenticityToken = new AuthenticityToken("mock-authenticity-token")
}