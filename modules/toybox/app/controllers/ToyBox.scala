package controllers

import play.api.GlobalSettings
import play.api.Configuration
import play.api.Plugin
import play.api.Play
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.Crypto
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.data.format.Formats._

import ToyBoxConfigKeys._


/** ToyBox provides a simple way to lock down Play 2.0 applications. 
 *  To use ToyBox in an application:
 *    0) Make sure the Global object of your project, if it exists, is in a package
 *    1) Mix ToyBox into your Global object
 *      1.5) Call super.onRouteRequest from your implementation of onRouteRequest
 *           if it's been overridden
 *    2) Add routes to your Global.getLogin and Global.postLogin
 *    3) Add abstract members of ToyBox to your Global object
 *    4) Configure through application.conf (see ToyBoxBase.scala for details)
 */
trait ToyBox extends DefaultTBBase with DefaultTBController with DefaultTBAuthenticator with DefaultTBGlobal




trait DefaultTBBase extends ToyBoxBase with GlobalSettings {
  // Delegate this to the actual ToyBox object so they can choose whether to default
  // the routes to defaultRoutePath (TODO: not yet implemented)
  def maybeGetLoginRoute: Option[Call]
  def maybePostLoginRoute: Option[Call]

  // Routes
  val defaultRoutePath = "/toybox/login"
 
  lazy val getLoginRoute  = maybeGetLoginRoute.getOrElse(new Call("GET", defaultRoutePath))
 
  lazy val postLoginRoute = maybePostLoginRoute.getOrElse(new Call("POST", defaultRoutePath))




  // General ToyBox configuration
  lazy val config = Play.current.configuration.getConfig("toybox").getOrElse(
    throw new Exception("ToyBox subconfiguration not present."))

  lazy val authUsername = config.getString(usrKey).getOrElse("")

  lazy val authPassword = config.getString(pwdKey).getOrElse(throw new Exception("No password configured."))

  lazy val isPrivate    = config.getBoolean(privateKey).getOrElse(true) 


  // Cookie configuration
  lazy val initialRequestCookieName = config.getString(initRequestKey).getOrElse("toybox-initial-request")

  lazy val authCookieName = config.getString(authCookieKey).getOrElse("toybox-authenticated")

  lazy val authTimeout    = config.getInt(authTimeoutKey).getOrElse(40*60)  // 40 minute default

  lazy val authPath       = config.getString(authPathKey).getOrElse("/")

  lazy val authDomain     = config.getString(authDomainKey)


  // Login Form
  val loginForm = Form( 
    tuple( 
      "username" -> text,
      "password" -> text
    )
  )
}




trait DefaultTBGlobal extends ToyBoxGlobal with GlobalSettings { 
  this: ToyBoxBase with ToyBoxController with ToyBoxAuthenticator =>

  /** Redirects unauthorized requests for private resources. 
   *  Authorized requests for private resources are handled different from 
   *  public resource requests because the authorization status is stored in
   *  a signed cookie that needs to be renewed periodically. 
   *
   *  TODO: catch log-in page request in the case where the maybe-endpoint-routes
   *  are not properly configured and manually serve the log-in page.
   */
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val forPublicResource = isPublicResourceRequest(request)
    val authorized = isAuthorized(request)

    if (forPublicResource) normalRouteRequestHandler(request)
    else if (authorized)   handleAuthorized(request)
    else                   redirectToLogin(request)
  }


/** Method pointing to the parent's onRouteRequtest method. Used to make testing easier. */
  protected def normalRouteRequestHandler: RequestHeader => Option[Handler] = super.onRouteRequest


  /** Handler for authorized requests to private resources. Sets a new authentication
   *  Cookie to keep it from expiring while the user is active.
   */
  protected def handleAuthorized(request: RequestHeader): Option[Handler] = {
    normalRouteRequestHandler(request) match {
      case Some(action: Action[AnyContent]) => Some(authenticate(action))
      case other => other
    }
  }


  /** Generates a redirect to the log-in page and sets a Cookie containing the method
   *  and path of the request received for later redirection, if none already exists.
   */
  protected def redirectToLogin(request: RequestHeader): Option[Handler] = Some( 
    Action {
      request.cookies.get(initialRequestCookieName) match {
      case Some(cookie: Cookie) => Redirect(getLoginRoute)
      case None => Redirect(getLoginRoute).withCookies( 
          makeInitialRequestCookie(request)) 
      }
    })

  /** Creates a Cookie holding the method and path of the given request */
  protected def makeInitialRequestCookie(request: RequestHeader): Cookie = {
    new Cookie(initialRequestCookieName, request.method + request.path, -1, "/", None, false, true)
  }
}




trait DefaultTBController extends ToyBoxController { 
  this: ToyBoxBase with ToyBoxAuthenticator =>

  // ToyBoxController methods
  def getLogin: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(postLoginRoute, loginForm))
  }


  def postLogin: Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formErrors   => BadRequest(views.html.login(postLoginRoute, formErrors)),

      loginAttempt =>
        if (isValidLogin(loginAttempt)) 
          Redirect(
              parseInitialRequestCookie(request)
            ).discardingCookies(
              initialRequestCookieName
            ).withCookies(
              makeAuthCookie(request)
            )
        else
          BadRequest(views.html.login(
            postLoginRoute, 
            loginForm, 
            previousUsername = loginAttempt._1,
            errorMessage = "Invalid username/password, please try again"
          ))
    )
  }

  /** Reads the method and path of the initial request Cookie if it's present. 
   *  Otherwise, the returned Call is a GET to the application root.
   */
  protected def parseInitialRequestCookie(request: Request[_]): Call = {
    request.cookies.get(initialRequestCookieName) match {
      case Some(Cookie(_, value, _, _, _, _, _)) =>
        new Call(value.takeWhile(_ != '/'), 
                 value.dropWhile(_ != '/'))
      case _ => new Call("GET", "/")
    }
  }
}





trait DefaultTBAuthenticator extends ToyBoxAuthenticator { this: ToyBoxBase =>
  lazy val publicPaths: Seq[String] = 
    Seq( // TODO: possibly pull extra paths from config
      getLoginRoute.url, 
      postLoginRoute.url, 
      "/assets/toybox"
    )

  def isAuthorized(request: RequestHeader) = {
    /** Checks for presence and authenticity of an authentication Cookie in a request */
    def hasAuthCookie = {
      request.cookies.get(authCookieName) match {
        case Some(Cookie(_, value, _, _, _, _, _)) if value.contains('=') => {
          val id = value.takeWhile(_ != '=')
          val signedId = value.dropWhile(_ != '=').tail
          signedId == Crypto.sign(id)
        }
        case _ => false
      }
    }

    !isPrivate || hasAuthCookie
  }

  def isPublicResourceRequest(request: RequestHeader) = {
    publicPaths.exists(request.path.startsWith(_))
  }



  /** Matches a given username/password pair against the configured credentials */
  def isValidLogin(usrPwdPair: (String, String)): Boolean = {
    // might want to ignore username if it's not configured (== ""), but not today... not today...
    usrPwdPair == (authUsername, authPassword)
  }


  /** Simple helper for adding an authentication Cookie to an action 
   *  Tried to parametrized the Action type, but couldn't get it to work...
   */
  protected def authenticate(action: Action[AnyContent]): Action[AnyContent] = Action { 
    implicit request => action(request) match {
      case plainResult: PlainResult => plainResult.withCookies(makeAuthCookie(request))
      case otherResult: Result => otherResult
    }
  }

  protected def makeAuthCookie(request: RequestHeader): Cookie = {
    val ip = request.remoteAddress
    val signedValue = ip + '=' + Crypto.sign(ip)
    new Cookie(authCookieName, signedValue, authTimeout, authPath, authDomain, false, false)
  }

}