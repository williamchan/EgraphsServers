package egraphs.toybox

import play.api.mvc.Cookie
import play.api.mvc.RequestHeader

abstract class ToyBoxCookieChecker {
  /**
   * Creates a new cookie, unless some implementation specific constraints
   * are not met.
   */
  def generate(request: RequestHeader): Option[Cookie]

  /**
   * Checks if a cookie is valid.
   */
  def validate(request: RequestHeader): Boolean
}