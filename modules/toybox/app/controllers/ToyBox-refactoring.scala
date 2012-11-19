/////////////////////////////////////// Interfaces ////////////////////////////////////////
trait ToyBox extends GlobalSettings { this: TBController =>
  // Routes
  val getLoginRoute: Call
  val postLoginRoute: Call

  // Configuration helpers
  def getUsername: String
  def getPassword: String
  def isPrivate: Boolean
  def authenticatedCookieName: String
  def initialRequestCookieName: String
  def authenticationTimeout: Int
  def publicPaths: Seq[String]

  // GlobalSettings helpers
  def isAuthorized(request: RequestHeader): Boolean
}

trait ToyBoxController extends Controller { this: ToyBox =>
  def getLogin:  Action[AnyContent]
  def postLogin: Action[AnyContent]
}


/////////////////////////////////////// Implementation ////////////////////////////////////////
package controllers {
class DefaultToyBox(val getLoginRoute:Call, val postLoginRoute:Call) extends ToyBox with ToyBoxController {
  // Configuration helpers
  private def config           = Play.current.configuration.getConfig("toybox")
  def getUsername              = config.getString("username").getOrElse("")
  def getPassword              = config.getString("password").getOrElse(throw new Exception("No password configured."))
  def isPrivate                = config.getBoolean("is-private").getOrElse(true)
  def authenticationTimeout    = 20*60  // 20 minutes
  def authenticatedCookieName  = config.getString("authenticated-cookie").getOrElse("toybox-authenticated")
  def initialRequestCookieName = config.getString("initial-request-cookie").getOrElse("toybox-initial-request")
  def publicPaths: Seq[String] = Seq(
    // TODO: possible pull extra paths from config
    getLoginRoute.path, 
    postLoginRoute.path, 
    "/assets/public/toybox-assets"
  )

  // GlobalSettings helpers
  def isAuthorized(request: RequestHeader): Boolean = {
    !isPrivate ||
    !isPrivateResourceRequest(request) ||
    hasAuthenticatedCookie(request)
  }

  def getLogin:  Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(postLoginRoute, loginForm))
  }

  def postLogin: Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formErrors   => BadRequest(views.html.login(postLoginRoute, formErrors))
      loginAttempt => {
        if (checkAuthentication(loginAttempt)) {
          addAuthentication(Redirect(parseInitialRequestCookie(request)))
        } else 
      }
    )
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    if (isAuthorized) {
      super.onRouteRequest(request) match {
        // don't want to renew authentication on public resource requests because
        // it makes log out a pain to implement (makes some sense semantically too)
        case Some(action: Action) if (isPrivateResourceRequest(request)) =>
          addAuthentication(action)
        case other => other
      }
    } else {
      redirectToLogin(request)
    }
  }

  // return true if request is within public paths or their children
  private def isPrivateResourceRequest(request: RequestHeader): Boolean = {
    publicPaths.exists(request.path.startsWith(_))
  }

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

  // result a redirect with the initial request path encoded in a cookie
  private def redirectToLogin(request: RequestHeader): Option[Handler] = Some( Action {
    Redirect(getLoginRoute).withCookies( makeInitialRequestCookie(request) )
  })

  // add an authentication cookie to the action
  private def addAuthentication(action: Action[AnyContent]]) = Action { implicit request =>
    action(request) match {
      case plainResult: plainResult => plainResult.withCookies(makeAuthenticationCookie(request))
      case result: Result => result
    }
  }
}}

trait ToyBoxGlobal extends GlobalSettings {
  def getLoginRoute: Call
  def postLoginRoute: Call
  val toyBox: ToyBox = new controllers.DefaultToyBox(getLoginRoute, postLoginRoute)

  override def onRouteRequest(request: RequestHeader): Option[Handler] = 
    toyBox.onRouteRequest(request)
}

