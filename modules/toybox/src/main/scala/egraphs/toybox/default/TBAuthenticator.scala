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
  // convenience values
  private val usernameKey = TBAuthenticator.usernameKey
  private val passwordKey = TBAuthenticator.passwordKey
  private val actualLogin = getLoginFromConfig()

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
        // TODO: change Ok and BadRequest to Redirect
        if (loginAttempt == actualLogin){
            // redirect to desired page
            // for now, return indication of success
            Ok("success")
        } else {
            // redirect to login page with error
            // for now, return indication of failure
            BadRequest("failure")
        }
      }
    )
  }

  private def getLoginFromConfig(): LoginDetails = {
    val username = config.getString(usernameKey).getOrElse(usernameKey + " not a key")
    val password = config.getString(passwordKey).getOrElse("")
    new LoginDetails(username, password)
  }
}

object TBAuthenticator {
  val usernameKey = "toybox-username"
  val passwordKey = "toybox-password"
}

case class LoginDetails(val username: String, val password: String) {
  require(password != null && password.length > 0)

  def == (that: LoginDetails): Boolean = {
    // let's say that if a username is not given, then usr can be anything
    that.password == this.password && 
      (that.username == this.username || this.username.length == 0)
  }
}