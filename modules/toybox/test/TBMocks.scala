package egraphs.toybox.tests

import play.api.Plugin
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._


import controllers._

object TBMocks {
  val loginPath = "/login"
	// fake toybox
	object FakeToyBox extends ToyBox with Plugin {
		def getLoginRoute  = new Call("GET", loginPath)
		def postLoginRoute = new Call("POST", loginPath)
		def actualUsername = "username"
		def actualPassword = "password"

    // overriding because TBMocks can't be initialized when using default
    // TBConfig methods since it wants to read Play.current.configuration
    override def authenticatedCookie = "toybox-authenticated"
    override def initialRequestCookie = "toybox-initialRequest"
    override def isPrivate = true
    override def authenticationTimeout = 20*60
	}

	// config data
	val usrKey 									= "toybox-username"
	val pwdKey 									= "toybox-password"
	val privateKey 							= "toybox-private"
	val authenticatedCookieKey   	= "toybox-authenticatedCookie"
	val initialRequestCookieKey = "toybox-initialRequestCookie"
	//val addPlugins 							= Seq("1000:FakeToyBox.plugin")
	val addConfig = Map(
		usrKey 									-> FakeToyBox.actualUsername,
		pwdKey 									-> FakeToyBox.actualPassword,
		privateKey 							-> "true",
		authenticatedCookieKey 		-> "toybox-authenticated",
		initialRequestCookieKey -> "toybox-initialRequest"
	)

	//val app = new FakeApplication(additionalPlugins=addPlugins, additionalConfiguration=addConfig)
  val fakeApp = new FakeApplication(additionalConfiguration=addConfig)

	// authCookie data
	val authCookieValue  = "should i be something special?"
	val authCookieMaxAge = 20*60 // 20 minutes
	val authCookiePath 	 = "/"   // should probably use login routes in some way
	def authCookie 			 = new Cookie(FakeToyBox.authenticatedCookie, authCookieValue, authCookieMaxAge, 
													authCookiePath, None, false, false) 

	val initCookieValue  = "/"
	val initCookieMaxAge = -1    // transient(?)
	val initCookiePath 	 = "/"   // should probably use login routes in some way
	def initCookie 			 = new Cookie(FakeToyBox.initialRequestCookie, initCookieValue, 
                          initCookieMaxAge, initCookiePath, None, false, false)

	// TODO: how to add headers??
	// requests to site
  val requestedPath = "/foobar"
  private val blankGetRequest = FakeRequest("GET", requestedPath)

	def unauthenticatedRequest = blankGetRequest
	val   authenticatedRequest = blankGetRequest.withCookies(authCookie)

	// requests for login without initial request authCookie
	val getLoginRequest = FakeRequest("GET", FakeToyBox.getLoginRoute.url)
  val getLoginAssetRequest = FakeRequest("GET", FakeToyBox.tbPlugin.loginAssetsPath + "/someAsset")
  private val blankPostLogin = FakeRequest("POST", FakeToyBox.postLoginRoute.url)
	val succLoginWithoutInitRequest = blankPostLogin.withFormUrlEncodedBody(
		"username" -> "username",
		"password" -> "password"
	)

	val failLoginWithoutInitRequest = blankPostLogin.withFormUrlEncodedBody(
		"username" -> "wrongusername",
		"password" -> "wrongpassword"
	)

	val succLoginWithInitRequest = succLoginWithoutInitRequest.withCookies(initCookie)
	val failLoginWithInitRequest = failLoginWithoutInitRequest.withCookies(initCookie)

  // TODO: can this be made generic (FakeRequest[A]?)
  def routeRequestToMaybeResult(req: FakeRequest[AnyContent]): Option[Result] = {
    FakeToyBox.tbPlugin.onRouteRequest(req) match {
      case Some(action: Action[AnyContent]) => Some(action(req))
      case _ => None
    }
  }

  def tryLogin(req: FakeRequest[AnyContentAsFormUrlEncoded]) = 
    FakeToyBox.tbController.postLogin(req)

  def resultSetsCookie(res: Result, cookieName: String): Boolean = {
    // there's probably a nicer way to do this
    res match {
      case plainRes: PlainResult => try {
          // value for SET_COOKIE header
          val headerValue = plainRes.header.headers(play.api.http.HeaderNames.SET_COOKIE)
          //println("set cookie header = " + headerValue)
          headerValue.startsWith(cookieName)
        } catch { 
          case _ => return false 
        }
      case _ => false
    }
  }
}