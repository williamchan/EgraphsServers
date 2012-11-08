package egraphs.toybox.default

import play.api.mvc.Cookie
import play.api.mvc.RequestHeader
import egraphs.toybox.ToyBoxCookieChecker

class TBCookieChecker(cookieName: String) extends ToyBoxCookieChecker {
    val maxCookieAge = 20*60      // 20 minutes

    /**
     * Generates the desired cookie. Default implementation only cares about the 
     * cookies name and age, but
     */
    def generateCookie(request: RequestHeader): Option[Cookie] = {
        // should the cookie contain any user information?
        // seems it only needs an age and AuthenticityToken takes care of the rest
        val cookieVal = "what should i be?"
        Option(mkCookie(cookieName, cookieVal, maxCookieAge))
    }


    /**
     * Checks if cookie is valid (exists). Default implementation just checks that it's 
     * not expired, but more elaborate use cases include storing tokens in the cookie 
     * and managing validity of token in some way, such as based on number of active users.
     */
    def validateCookie(request: RequestHeader): Boolean = {
        request.cookies.get(cookieName).getOrElse(null) != null
    }

    private def mkCookie(name: String = "", value: String = "", maxAge: Int) = 
        new Cookie(name, value, maxAge, "/", Option(null), false, false)
}