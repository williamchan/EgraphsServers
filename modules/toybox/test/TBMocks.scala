package egraphs.toybox.tests

import play.api.Plugin
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._


import controllers._
import controllers.ToyBoxConfigKeys._

object TBMocks {
  val loginPath = "/login"
  val loginAssetsPath = "/assets/toybox-assets"

  val fakeApp = new FakeApplication(
    additionalConfiguration = Map(
      usrKey         -> "username",
      pwdKey         -> "password",
      initRequestKey -> "toybox-initialRequest",
      authCookieKey  -> "toybox-authenticated",
      authTimeoutKey -> (19*60 toString)
    )
  )


  // Mock ToyBox
  object FakeToyBox extends ToyBox {
    val maybeGetLoginRoute  = Some(new Call("GET", loginPath))
    val maybePostLoginRoute = Some(new Call("POST", loginPath))

    // mocking up a subconfiguration is tedious, overriding config base/root is 
    // virtually the same since there are no other unexpected configurations present 
    override lazy val config = fakeApp.configuration
    override def normalRouteRequestHandler: RequestHeader => Option[Handler] = {
      (request: RequestHeader) => Some(Action{Ok("fake normal route request")})
    }

    // expose cookies making methods for mocking requests
    def authCookie(req: RequestHeader)        = makeAuthCookie(req)
    def initRequestCookie(req: RequestHeader) = makeInitialRequestCookie(req)
  }


  // Mock requests
  val requestedPath    = "/foobar"
  val blankGetRequest  = FakeRequest("GET", requestedPath)
  val blankPostRequest = FakeRequest("POST", requestedPath)

  val getLoginAssetRequest = FakeRequest("GET", loginAssetsPath + "/someAsset")
  val getLoginRequest = FakeRequest("GET", FakeToyBox.getLoginRoute.url)
  val blankPostLogin  = FakeRequest("POST", loginPath)
  val succPostLogin   = blankPostLogin.withFormUrlEncodedBody("username" -> "username", "password" -> "password")
  val failPostLogin   = blankPostLogin.withFormUrlEncodedBody("username" -> "wrongusername", "password" -> "wrongpassword")


  // Mock cookies
  def authCookie = FakeToyBox.authCookie(blankPostRequest)
  def initCookie = FakeToyBox.initRequestCookie(blankGetRequest)
  def initCookiePath = initCookie.value.dropWhile(_ != '/') // path of initial request, not cookie.path


  // Helpers for adding mock cookies to requests
  def authenticated(req: FakeRequest[AnyContent]) = req.withCookies(authCookie)
  def withInit[T <: AnyContent](req: FakeRequest[T]): FakeRequest[T] = 
    req.withCookies(initCookie)

  /** Takes a request and returns the result of applying its handler to itself as a 
   *  simple result. Throws an exception for no handler or non-SimpleResults.
   */
  def routeRequestToSimpleResult[T <: AnyContent](req: FakeRequest[T]): PlainResult = {
    FakeToyBox.onRouteRequest(req) match {
      case Some(action: Action[T]) => 
        action(req) match {
          case plain: PlainResult => plain
          case _ => throw new ClassCastException("Unexpected result type for route request.")
        }
      case None => throw new Exception("No result for route request.")
    }
  }

  /** Returns the result of applying a request to the postLogin endpoint of the login page */
  def tryLogin[T <: AnyContent](req: FakeRequest[T]): PlainResult = 
    FakeToyBox.postLogin(req) match {
      case plain: PlainResult => plain
      case _ => throw new Exception("Login attempt result was not a PlainResult")
    }

  /** Checks if a result has the SET_COOKIE header for the given cookie name */
  def resultSetsCookie(res: Result, cookieName: String): Boolean = {
    // there's probably a nicer way to do this
    res match {
      case plainRes: PlainResult => 
        try {
          // value for SET_COOKIE header
          val headerValue = plainRes.header.headers(play.api.http.HeaderNames.SET_COOKIE)
          headerValue.contains(cookieName)
        } catch { 
          case _ => return false 
        }
      case _ => false
    }
  }
}