/**
 * Default implementations for ToyBox
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
        Option(null)
    }
}

/*****************************************************************************/

/**
 * Default public toybox, does nothing
 */
case class TBPublic extends ToyBox {
    def serviceRouteRequest(req: RequestHeader): Option[Handler] = {
        // return normal handler
        Option(null)
    }
}