package egraphs.authtoken

import play.api.libs.Crypto

class AuthenticityToken(val value: String)

object AuthenticityToken 
  extends AuthenticityTokenActionComposition 
  with AuthenticityTokenFormHelpers 
{
  //
  // AuthenticityTokenActionComposition members
  //
  override protected def newAuthTokenString = {
    Crypto.sign(java.util.UUID.randomUUID.toString)
  }

  //
  // Private members
  //
  private[authtoken] val authTokenKey = "authenticityToken"  
}
