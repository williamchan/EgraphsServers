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
import org.joda.time.DateTimeConstants._

import ToyBoxConfigKeys._


/** ToyBox provides a simple way to lock down Play 2.0 applications. 
 *  See README for usage instructions.
 */
trait ToyBox extends DefaultTBBase with DefaultTBController with DefaultTBAuthenticator




/** Default ToyBoxBase implementation */
trait DefaultTBBase extends ToyBoxBase with GlobalSettings {
  this: ToyBoxController with ToyBoxAuthenticator =>

  // Routes
  lazy val getLoginRoute = new Call("GET", loginPath)
  lazy val postLoginRoute = new Call("POST", loginPath)
  val toyboxAssetsDirectory = "toybox-assets/"

  // General ToyBox configuration
  lazy val config = Play.current.configuration.getConfig("toybox").getOrElse(
    throw new IllegalStateException("ToyBox subconfiguration not present."))
  
  lazy val authPassword = config.getString(passwordKey).getOrElse(
    if (isPrivate) throw new IllegalStateException("No password configured.")
    else ""
  )

  lazy val authUsername = config.getString(userKey).getOrElse("")
  lazy val isPrivate    = config.getBoolean(privateKey).getOrElse(true) 

  // iPad authorization configuration
  lazy val iPadHeader: Option[String] = config.getString(iPadHeaderKey)
  lazy val iPadSecret: Option[String] = config.getString(iPadSecretKey)


  /** Paths to public assets and pages. Could also pull more paths from config or replace
   *  with Seq[RequestHeader => Boolean] to allow for more flexible exceptions.
   */
  lazy val publicAccessConditions: Seq[RequestHeader => Boolean] = 
    Seq( 
      // GET login
      { (request: RequestHeader) => request.method.toLowerCase == "get" && 
          request.path.startsWith(getLoginRoute.url) }, 

      // POST login
      { (request: RequestHeader) => request.method.toLowerCase == "post" && 
          request.path.startsWith(postLoginRoute.url) }, 

      // ToyBox assets
      { (request: RequestHeader) => 
        request.path.startsWith(assetsRoute(toyboxAssetsDirectory).url) },

      // iPad authentication
      { (request: RequestHeader) =>
        { for (
            headerName <- iPadHeader;
            headerVal <- request.headers.get(headerName);
            secret <- iPadSecret
          ) yield { secret == headerVal }
        }.getOrElse(false)
      }
    )

  
  // Cookie configuration
  lazy val initialRequestCookieName = config.getString(initRequestKey).getOrElse("toybox-initial-request")
  lazy val authCookieName = config.getString(authCookieKey).getOrElse("toybox-authenticated")
  lazy val authTimeoutInSeconds = config.getInt(authTimeoutInSecondsKey).getOrElse(SECONDS_PER_DAY)
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

    if (isLoginRequest(request)) {
      Some(loginHandler(request))
    
    } else if (isPublicResourceRequest(request)) {
      normalRouteRequestHandler(request)
    
    } else if (isAuthorized(request)) {
      handleAuthorized(request)
    
    } else {
      Some(redirectToLogin(request))
    }
  }

  protected def isLoginRequest(request: RequestHeader) = { 
    val forLoginPath = request.method match {
      case "GET" => request.path == getLoginRoute.url
      case "POST" => request.path == postLoginRoute.url
      case _ => false 
    } 
    forLoginPath && isPrivate
  }

  protected def loginHandler(request: RequestHeader): Handler = {
    if (request.method == "GET") 
      getLogin
    else
      postLogin
  }

  /** Method pointing to the parent's onRouteRequest method. Used to make testing easier. */
  protected def normalRouteRequestHandler: (RequestHeader => Option[Handler]) = super.onRouteRequest


  /** Handler for authorized requests to private resources. Sets a new authentication
   *  Cookie to keep it from expiring while the user is active.
   */
  protected def handleAuthorized(request: RequestHeader): Option[Handler] = {
    val handler: Option[Handler] = normalRouteRequestHandler(request)
    (request.contentType, handler) match {
      case (Some("multipart/form-data"), _) => {
        /**
         * We need to handle this as a special case otherwise the subsequent line will try to cast
         * multipart form POSTs to AnyContent, and we get the following exception message:
         * ClassCastException: play.api.mvc.AnyContentAsMultipartFormData cannot be cast to play.api.mvc.MultipartFormData.
         *
         * This exception can be reproduced by deleting this case and attempting a multipart form POST.
         * Checking the contentType of the request allows us to identify multipart form POSTs without triggering
         * a ClassCastException.
         *
         * The issue stems from that the type parameter of the action is erased by the JVM at runtime, so we cannot
         * pattern match on type parameters. We also considered using a scala.reflect.Manifest, which is a class
         * that represents Scala types, but decided against that since this is the only instance of this issue.
         * http://stackoverflow.com/questions/1094173/how-do-i-get-around-type-erasure-on-scala-or-why-cant-i-get-the-type-paramete
         */
        handler
      }
      case (_, Some(action: Action[AnyContent])) => Some(authenticate(action))
      case (_, other) => other
    }
  }


  /** Generates a redirect to the log-in page and sets a Cookie containing the method
   *  and path of the request received for later redirection, if none already exists.
   */
  protected def redirectToLogin(request: RequestHeader): Handler = Action {
    request.cookies.get(initialRequestCookieName) match {
    case Some(cookie: Cookie) => Redirect(getLoginRoute)
    case None => Redirect(getLoginRoute).withCookies( 
        makeInitialRequestCookie(request)) 
    }
  }

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
    request.cookies.get(authCookieName) match {
      case Some(Cookie(_, signature, _, _, _, _, _)) =>
        signature == Crypto.sign(request.remoteAddress)
      case _ => false
    }
  }

  /** Checks if a request is for a public resource */
  def isPublicResourceRequest(request: RequestHeader) = {
    !isPrivate || publicAccessConditions.exists(_.apply(request))
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

  /** Keys for iPad head and secret (for iPad authentication) */
  val iPadHeaderKey = "ipad-header"
  val iPadSecretKey = "ipad-secret"
}