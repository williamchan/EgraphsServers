package controllers

import play.api._
import play.api.mvc._

/* 
Design considerations:
-should any of the helpers be moved to a companion object?

TODO:
-fill in stubs
-sign cookies
-make necessary changes to tests
  -add tests for cookie signing and log out
-make login view prettier and more functional
*/

trait ToyBox extends ToyBoxBase with ToyBoxController with GlobalSettings {

  // ROUTES
  val getLoginRoute:  Option[Call] 
  val postLoginRoute: Option[Call]

  // CONFIGURATION
  def getUsername              = config.getString("username").getOrElse("")
  def getPassword              = config.getString("password").getOrElse(throw new Exception("No password configured."))
  def isPrivate                = config.getBoolean("is-private").getOrElse(true)
  def authenticationTimeout    = 20*60  // 20 minutes
  def authenticatedCookieName  = config.getString("authenticated-cookie").getOrElse("toybox-authenticated")
  def initialRequestCookieName = config.getString("initial-request-cookie").getOrElse("toybox-initial-request")
  def publicPaths: Seq[String] = Seq( // TODO: possible pull extra paths from config
    getLoginRoute.path, 
    postLoginRoute.path, 
    "/assets/public/toybox-assets"
  )

  // CONTROLLER METHODS
  def getLogin: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(postLoginRoute, loginForm))
  }

  /* 
  add functionality
  -meaningul errors
  -replace username on failed log in
  */
  def postLogin: Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formErrors   => BadRequest(views.html.login(postLoginRoute, formErrors)),
      loginAttempt =>
        if (checkAuthentication(loginAttempt))
          addAuthentication(Redirect(parseInitialRequestCookie(request)))
        else
          BadRequest(views.html.login(postLoginRoute, loginForm))
    )
  }

  // GLOBALSETTINGS METHODS
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    if (!isPrivateResourceRequest(request)) super.onRouteRequest(request)
    else if (isAuthorized)                  handleAuthorized(request)
    else                                    redirectToLogin(request)
  }

  // HELPERS
  private val config = Play.current.configuration.getConfig("toybox")

  def isAuthorized(request: RequestHeader): Boolean = {
    !isPrivate || hasAuthenticatedCookie(request) 
  }

  // return true if request is within public paths or their children
  private def isPrivateResourceRequest(request: RequestHeader): Boolean = {
    publicPaths.exists(request.path.startsWith(_))
  }

  // return true if login form submission matches configured credentials
  private def checkAuthentication(usrPwdPair: (String, String)) = {
    // might want to ignore username if it's not configured (== ""), but not today... not today...
    usrPwdPair == (getUsername, getPassword)
  }

  // return true if request has a valid authentication cookie
  private def hasAuthenticatedCookie(request: RequestHeader): Boolean = {
    request.cookies.get(authenticatedCookieName) match {
      // TODO: check that the value is legitimate
      case Some(Cookie(_, value, _, _, _, _, _)) => true 
      case None                                  => false
    }
  }

  // helpers for creating an authetication cookie for a request
  private def makeAuthenticationCookie(request: RequestHeader): Cookie = {
    new Cookie(authenticatedCookieName, 
      "TODO: use a meaningful value",
      authenticationTimeout, "/", None, false, false)
  }

  // helper for creating a cookie to store initial request for redirection after login
  private def makeInitialRequestCookie(request: RequestHeader): Cookie = {
    new Cookie(initialRequestCookieName, request.method + request.path, -1, "/", None, false, false)
  }

  // helper for determining redirect destination after login
  private def parseInitialRequestCookie(request: Request[_]): Call = {
    req.cookies.get(initialRequestCookieName) match {
      case Some(Cookie(_, value, _, _, _, _, _)) =>
        val method = value.takeWhile(_ != '/')
        val dest = value.stripPrefix(method)
        new Call(method, dest)
      case _ => new Call("GET", "/")
    }
  }

  private def handleAuthorized(request: RequestHeader): Option[Handler] = {
    Option( super.onRouteRequest(request) match {
      case Some(action: Action) => addAuthentication(action)
      case other                => other
    })
  }

  // redirect with the initial request path encoded in a cookie
  private def redirectToLogin(request: RequestHeader): Option[Handler] = 
    Some( Action {Redirect(getLoginRoute).withCookies( makeInitialRequestCookie(request) ) })

  // add an authentication cookie to the action
  private def addAuthentication(action: Action[AnyContent]]) = Action { implicit request =>
    action(request) match {
      case plainResult: plainResult => plainResult.withCookies(makeAuthenticationCookie(request))
      case result: Result => result
    }
  }

  // FORM
  val loginForm = Form( 
    tuple( 
      "Username" -> nonEmptyText,
      "Password" -> nonEmptyText
    )
  )
}