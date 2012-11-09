/**
 * Default implementations for ToyBox
 *
 * Perhaps rename to TBox or ToyBoxDefault?
 */

package egraphs.toybox.default

import egraphs.toybox.ToyBox
import egraphs.toybox.ToyBoxAuthenticator
import egraphs.toybox.ToyBoxCookieChecker

import play.api.mvc.Handler
import play.api.mvc.RequestHeader

/**
 * Default private toybox
 */
case class TBPrivate(authenticator: ToyBoxAuthenticator, cookieChecker: ToyBoxCookieChecker)
  extends ToyBox {
  def serviceRouteRequest(req: RequestHeader): Option[Handler] = {
    // if authenticated, 
    if (cookieChecker.validate(req))
      None
    else
      // TODO: redirect to login
      None
  }
}

/*****************************************************************************/

/**
 * Default public toybox, does nothing
 */
case class TBPublic extends ToyBox {
  def serviceRouteRequest(req: RequestHeader): Option[Handler] = {
    // doesn't need a special handler
    None
  }
}