/**
 * Defines some mostly-minimal requirements for ToyBox
 */
trait ToyBoxBase { this: ToyBoxController with GlobalSettings =>
  // Routes
  val getLoginRoute:  Option[Call]
  val postLoginRoute: Option[Call]

  // Configuration helpers
  def getUsername: String
  def getPassword: String
  def isPrivate: Boolean
  def authenticatedCookieName: String
  def initialRequestCookieName: String
  def authenticationTimeout: Int
  def publicPaths: Seq[String]

  /* 
  would it make more sense for this to extend GlobalSettings and
  override onRouteRequest in terms of some abstract methods? Ex:

    def isAuthenticated(request): Boolean
    def handleRequest(request: RequestHeader): Option[Handler]
    def redirectToLogin(request: RequestHeader): Option[Handler]
    override def onRouteRequest(request: RequestHeader): Option[Handler] =
      if (isAuthenticated request) handleRequest(request)
      else                         redirectToLogin(request)

  This makes the need to override onRouteRequest explicit (a good alt would be
  if you can override onRouteRequest into an abstract method -- doesn't seem 
  you can do that), but is otherwise a constraint
  */
}

/**
 * Defines the endpoints for ToyBox login page
 */
trait ToyBoxController extends Controller { this: ToyBox =>
  def getLogin:  Action[AnyContent]
  def postLogin: Action[AnyContent]
}
