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

/** ToyBox allows the locking down of Play 2.0 applications 
 *
 */
trait ToyBox extends ToyBoxBase with ToyBoxController with GlobalSettings {

  // Routes
  val defaultRoutePath = "/toybox/login"
 
  def maybeGetLoginRoute:  Option[Call] 
 
  def maybePostLoginRoute: Option[Call]
 
  lazy val getLoginRoute  = maybeGetLoginRoute.getOrElse(new Call("GET", defaultRoutePath))
 
  lazy val postLoginRoute = maybePostLoginRoute.getOrElse(new Call("POST", defaultRoutePath))

 

  // General ToyBox configuration
  lazy val config = Play.current.configuration.getConfig("toybox").getOrElse(
    throw new Exception("ToyBox subconfiguration not present."))

  lazy val authUsername = config.getString(usrKey).getOrElse("")

  lazy val authPassword = config.getString(pwdKey).getOrElse(throw new Exception("No password configured."))

  lazy val isPrivate    = config.getBoolean(privateKey).getOrElse(true)

  lazy val publicPaths: Seq[String] = 
    Seq( // TODO: possibly pull extra paths from config
      getLoginRoute.url, 
      postLoginRoute.url, 
      "/assets/toybox"
    ) 


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



  // ToyBoxController methods
  def getLogin: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(postLoginRoute, loginForm))
  }


  def postLogin: Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formErrors   => BadRequest(views.html.login(postLoginRoute, formErrors)),

      loginAttempt =>
        if (checkLoginAttempt(loginAttempt)) 
          Redirect(
            parseInitialRequestCookie(request)
          ).withCookies(
            makeAuthCookie(request)
          ).discardingCookies(
            initialRequestCookieName
          )
        else
          BadRequest(views.html.login(postLoginRoute, loginForm, loginAttempt._1))
    )
  }


  /** Matches a given username/password pair against the configured credentials */
  protected def checkLoginAttempt(usrPwdPair: (String, String)) = {
    // might want to ignore username if it's not configured (== ""), but not today... not today...
    usrPwdPair == (authUsername, authPassword)
  }

  




  // GlobalSettings methods

  /** Redirects unauthorized requests for private resources. 
   *  Authorized requests for private resources are handled different from 
   *  public resource requests because the authorization status is stored in
   *  a signed cookie that needs to be renewed periodically. 
   *
   *  TODO: catch log-in page request in the case where the maybe-endpoint-routes
   *  are not properly configured and manually serve the log-in page.
   */
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    def isPublicResourceRequest = publicPaths.exists(request.path.startsWith(_))
    def isAuthorized = !isPrivate || hasAuthCookie(request) 

    if (isPublicResourceRequest) normalRouteRequestHandler(request)
    else if (isAuthorized)       handleAuthorized(request)
    else                         redirectToLogin(request)
  }


/** Method pointing to the parent's onRouteRequtest method. Used to make testing easier. */
  protected def normalRouteRequestHandler: RequestHeader => Option[Handler] = super.onRouteRequest


  /** Handler for authorized requests to private resources. Sets a new authentication
   *  Cookie to keep it from expiring while the user is active.
   */
  protected def handleAuthorized(request: RequestHeader): Option[Handler] = {
    normalRouteRequestHandler(request) match {
      case Some(action: Action[AnyContent]) => Some(addAuthentication(action))
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


  /** Checks for presence and authenticity of an authentication Cookie in a request */
  protected def hasAuthCookie(request: RequestHeader): Boolean = {
    request.cookies.get(authCookieName) match {
      // TODO: check that the value is legitimate
      case Some(Cookie(_, value, _, _, _, _, _)) => value == Crypto.sign(request.remoteAddress)
      case None                                  => false
    }
  }


  /** Simple helper for adding an authentication Cookie to an action 
   *  Tried to parametrized the Action type, but couldn't get it to work...
   */
  protected def addAuthentication(action: Action[AnyContent]): Action[AnyContent] = Action { 
    implicit request => action(request) match {
      case plainResult: PlainResult => plainResult.withCookies(makeAuthCookie(request))
      case result: Result => result
    }
  }






  /** Creates an authentiticy Cookie for a given request */
  protected def makeAuthCookie(request: RequestHeader): Cookie = {
    val signedValue = { Crypto.sign(request.remoteAddress) }
    new Cookie(authCookieName, signedValue, authTimeout, authPath, authDomain, false, false)
  }


  /** Checks authentication cookie value against signed IP address */
  protected def checkAuthCookieValue(sig: String): Boolean = {
    val uuid = sig.takeWhile(_ != '=')
    val signedUuid = sig.dropWhile(_ != '=').tail
    Crypto.sign(uuid) == signedUuid
  }


  /** Creates a Cookie holding the method and path of the given request */
  protected def makeInitialRequestCookie(request: RequestHeader): Cookie = {
    new Cookie(initialRequestCookieName, request.method + request.path, -1, "/", None, false, true)
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