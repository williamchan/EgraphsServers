package egraphs.toybox.default

import egraphs.toybox.ToyBoxAuthenticator
import play.api.Configuration
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.data.format.Formats._

/**
 * TODO: enforce the compile-time requirement of having a valid route to
 * the login page being used.
 *      -accomodate the use of TBAuthenticator with an arbitrary login route
 */

class TBAuthenticator(config: Configuration) extends ToyBoxAuthenticator {
    // this should probably be a def to allow changing username/password at runtime
    private def actualLogin: LoginDetails = AuthenticatorMockUtil.getLoginFromConfig()

    /**
     * Form for login submission, maps the input to a LoginDetails object
     */
    val loginForm = Form( 
        Forms.mapping(
            "username" -> Forms.of[String],       // not necessarily required
            "password" -> nonEmptyText      // must be non-empty
        )(LoginDetails.apply)(LoginDetails.unapply)
    )

    // GET login page
    def login = Action {
        // TODO: want to add a cookie for the original requested page by this point at latest
        Ok("login")
    }

    // POST authentication
    def  authenticate = Action { implicit request =>
        loginForm.bindFromRequest.fold(
            errors => BadRequest("errors"),

            loginAttempt => {
                if (loginAttempt == actualLogin){
                    // redirect to desired page
                    // for not, return indication of success
                    Ok("success")
                } else {
                    // redirect to login page with error
                    // for now, return indication of failure
                    BadRequest("failure")
                }
                
            }
        )
    }


    private val usernameKey = "toybox-username"
    private val passwordKey = "toybox-password"
    private def getLoginFromConfig(): LoginDetails = {
        val maybeUsername = 
            if (config.keys.contains(usernameKey))
                config.getString(usernameKey)
            else Option(null)
        val maybePassword = 
            if (config.keys.contains(passwordKey))
                config.getString(passwordKey)
            else Option(null)

        new LoginDetails(
            maybeUsername.getOrElse(""),
            maybePassword.getOrElse("")
        )
    }
}

case class LoginDetails(username: String, password: String) {
    require(password.length > 0)

    def == (that: LoginDetails): Boolean = {
        // let's say that if a username is not given, then usr can be anything
        that.password == this.password && 
            (that.username == this.username || this.username.length == 0) 
    }
}


object AuthenticatorMockUtil {
    def getLoginFromConfig(): LoginDetails = new LoginDetails("username", "password")
}