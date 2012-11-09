package egraphs.toybox.default

import play.api.mvc.Cookie
import play.api.mvc.RequestHeader
import egraphs.toybox.ToyBoxCookieChecker

/* 
 * TODO: read maxCookieAge from config or somethiiiing like that
 */

class TBCookieChecker(cookieName: String) extends ToyBoxCookieChecker {
  // require non-null, non-empty cookie name
  require(cookieName != null && cookieName.length > 0)
  val maxCookieAge = 20*60      // 20 minutes

  /**
   * Generates the desired cookie. Default implementation only cares about the 
   * cookies name and age, but
   */
  def generate(request: RequestHeader): Option[Cookie] = {
    // should the cookie contain any user information?
    // seems it only needs an age and AuthenticityToken takes care of the rest
    val cookieVal = "you could make me a token if you wanted!"
    Option(mkCookie(cookieName, cookieVal, maxCookieAge))
  }

  /**
   * Checks if cookie is valid (exists). Default implementation just checks that it's 
   * not expired, but more elaborate use cases include storing tokens in the cookie 
   * and managing validity of token in some way, such as based on number of active users.
   */
  def validate(request: RequestHeader): Boolean = {
    request != null && request.cookies.get(cookieName) != None
  }

  private def mkCookie(name: String = "", value: String = "", maxAge: Int) = 
    new Cookie(name, value, maxAge, "/", Option(null), false, false)
}