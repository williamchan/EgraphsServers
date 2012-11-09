package egraphs.toybox.default

import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.mvc.Cookie

object TBMockObjects {
  val app = new FakeApplication(additionalConfiguration = 
    Map(
      TBAuthenticator.usernameKey -> "username",
      TBAuthenticator.passwordKey -> "password"
    )
  )

  val cookieName = "ToyBox-Cookie"
  val auth = new TBAuthenticator(app.configuration)
  val checker = new TBCookieChecker(cookieName)


  val authCookie = checker.generate(FakeRequest()).getOrElse(null)

  // mock GET requests
  val unauthRequest = FakeRequest()
  val   authRequest = FakeRequest().withCookies(authCookie)

  // mock POST requests for login
  // (should there be mock POST requests other than for login?)
  val loginFailureRequest = FakeRequest().withFormUrlEncodedBody(
    "username" -> "wrongusername",
    "password" -> "wrongpassword"
  )

  val loginSuccessRequest = FakeRequest().withFormUrlEncodedBody(
    "username" -> "username",
    "password" -> "password"
  )
}