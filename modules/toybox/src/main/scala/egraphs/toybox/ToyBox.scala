package egraphs.toybox

import play.api.Play
import play.api.mvc.Handler
import play.api.mvc.RequestHeader

// privacy modes, add helpers somewhere in this family of classes
abstract class ToyBox {

    /**
     * Services route requests in ToyBoxGlobal
     */
    def serviceRouteRequest(req: RequestHeader): Option[Handler]
}

/*****************************************************************************/

/**
 * Essentially a ToyBoxFactory for default implementations. Note that to use default
 * implementations of ToyBox with custom implementations of ToyBoxAuthentication or
 * ToyBoxTokenStore, include them individually from this file and write your own 
 * ToyBox object.
 *
 * Will probably change the means of creating a ToyBox to something more explicit.
 */
object ToyBox {
    val defaultCookieName = "toybox-auth"

    def apply(): ToyBox = {
        new egraphs.toybox.default.TBPrivate(
            new egraphs.toybox.default.TBAuthenticator(Play.current.configuration),
            new egraphs.toybox.default.TBCookieChecker(defaultCookieName)
        )
    }
}










    /**
     * Key value for privacy setting in config
     */
    // private val privacyKey = "toybox-private"
    // private val usernameKey = "toybox-username"
    // private val passwordKey = "toybox-password"

    /**
     * Determine if application defaults to private for invalid privacy configuration
     */
    // private val defaultPrivate = true

    /**
     * read config and return correct implementation of ToyBoxMode
     */
    /* def apply(app: Application): ToyBox = {
        val config = app.configuration
        val hasPrivacyKey = config.keys.contains(privacyKey)
        val hasUsername = config.keys.contains(usernameKey)
        val hasPassword = config.keys.contains(passwordKey)

        val isPrivate: Option[Boolean] = 
            if (hasPrivacyKey) Option(config.getBoolean(privacyKey))
            else               Option(defaultPrivate)
        

        if (true) //(isPrivate.getOrElse(false) && hasUsername && hasPassword) 
            new TBPrivate(
                new ToyBoxAuthenticator(config), 
                new ToyBoxTokenChecker("toybox-cookie"))
        else 
            new TBPublic
    }*/
