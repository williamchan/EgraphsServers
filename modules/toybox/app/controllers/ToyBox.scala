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
 *  See README for usage instructions.
 */
trait ToyBox extends DefaultTBBase with DefaultTBController with DefaultTBAuthenticator




/** Default ToyBoxBase implementation */
trait DefaultTBBase extends ToyBoxBase with GlobalSettings {
  this: ToyBoxController with ToyBoxAuthenticator =>

  // Delegate this to the actual ToyBox object so they can choose whether to default
  // the routes to defaultLoginPath nicely by setting to None
  def maybeGetLoginRoute: Option[Call]
  def maybePostLoginRoute: Option[Call]
  def maybeAssetsRoute: Option[String => Call]

  // Routes
  val defaultLoginPath = "/toybox/login"
  val defaultAssetsPath = "/assets/toybox-assets/"
  lazy val getLoginRoute  = maybeGetLoginRoute.getOrElse(new Call("GET", defaultLoginPath))
  lazy val postLoginRoute = maybePostLoginRoute.getOrElse(new Call("POST", defaultLoginPath))
  lazy val assetsRoute = maybeAssetsRoute.getOrElse({ (file: String) => 
    new Call("GET", defaultAssetsPath + implicitly[PathBindable[String]].unbind("file", file))
  })


  // General ToyBox configuration
  lazy val config = Play.current.configuration.getConfig("toybox").getOrElse(
    throw new IllegalStateException("ToyBox subconfiguration not present."))
  lazy val authPassword = config.getString(passwordKey).getOrElse(
    throw new IllegalStateException("No password configured."))

  lazy val authUsername = config.getString(userKey).getOrElse("")
  lazy val isPrivate    = config.getBoolean(privateKey).getOrElse(true) 


  /** Paths to public assets and pages. Could also pull more paths from config or replace
   *  with Seq[RequestHeader => Boolean] to allow for more flexible exceptions.
   */
  lazy val publicAccessConditions: Seq[RequestHeader => Boolean] = 
    Seq( 
      { (request: RequestHeader) => request.method.toLowerCase == "get" && 
          request.path.startsWith(getLoginRoute.url) }, 

      { (request: RequestHeader) => request.method.toLowerCase == "post" && 
          request.path.startsWith(postLoginRoute.url) }, 

      { (request: RequestHeader) => request.path.startsWith("/assets/toybox-assets") }
    )

  
  // Cookie configuration
  lazy val initialRequestCookieName = config.getString(initRequestKey).getOrElse("toybox-initial-request")
  lazy val authCookieName = config.getString(authCookieKey).getOrElse("toybox-authenticated")
  lazy val authTimeoutInSeconds = config.getInt(authTimeoutInSecondsKey).getOrElse(40*60)  // 40 minute default
  lazy val authPath = config.getString(authPathKey).getOrElse("/")
  lazy val authDomain = config.getString(authDomainKey)


  // Login Form
  val loginForm = Form( 
    tuple( 
      "username" -> text,
      "password" -> text
    )
  )


  /** Redirects unauthorized requests for private resources. 
   *  Authorized requests for private resources are handled different from 
   *  public resource requests because the authorization status is stored in
   *  a signed cookie that needs to be renewed periodically.
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





/** Default get and post endpoint controller implementation for ToyBox */
trait DefaultTBController extends ToyBoxController { 
  this: ToyBoxBase with ToyBoxAuthenticator =>

  // ToyBoxController methods
  def getLogin: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(postLoginRoute, assetsRoute, loginForm))
  }


  def postLogin: Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formErrors   => BadRequest(views.html.login(postLoginRoute, assetsRoute, formErrors)),

      loginAttempt =>
        if ( loginAttempt == (authUsername, authPassword)) 
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
            assetsRoute,
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
  /** Checks if a request is authorized to access protected resources */
  def isAuthorized(request: RequestHeader) = {
    // Checks for signed authentication Cookie in a request
    val hasAuthCookie = 
      request.cookies.get(authCookieName) match {
        case Some(Cookie(_, signature, _, _, _, _, _)) =>
          signature == Crypto.sign(request.remoteAddress)
        case _ => false
      }

    !isPrivate || hasAuthCookie
  }

  /** Checks if a request is for a public resource */
  def isPublicResourceRequest(request: RequestHeader) = {
    publicAccessConditions.exists(_.apply(request))
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

  /** Make signed authentication cookie. Currently, signs ip against application secret. 
   */
  protected def makeAuthCookie(request: RequestHeader): Cookie = {
    val ip = request.remoteAddress
    val signedValue = Crypto.sign(ip)
    new Cookie(authCookieName, signedValue, authTimeoutInSeconds, authPath, authDomain, false, false)
  }

}




/** Store of keys for querying configuration */
object ToyBoxConfigKeys {
  /** Key to username credential for login */
  val userKey = "username"

  /** Key to password credential for login */
  val passwordKey = "password"

  /** Key to flag for application privacy */
  val privateKey = "is-private"

  /** Key to cookie name for saving initial request for redirection after login */
  val initRequestKey = "initial-request-cookie"

  /** Key to authentication cookie name */
  val authCookieKey = "auth-cookie"

  /** Key to authentication cookie timeout in seconds */
  val authTimeoutInSecondsKey = "auth-timeout"

  /** Key to authentication cookie path */
  val authPathKey = "auth-path"

  /** Key to authentication cookie domain */
  val authDomainKey = "auth-domain"
}