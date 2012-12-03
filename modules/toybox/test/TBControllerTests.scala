package egraphs.toybox.tests

import egraphs.toybox.tests.TBFakes._
import controllers._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.Status
import play.api.test._

import play.api.test.Helpers._

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner

import java.lang.ExceptionInInitializerError

/** Tests the implementation of the methods of the ToyBoxController trait */
@RunWith(classOf[JUnitRunner])
class DefaultTBControllerTests extends FlatSpec with ShouldMatchers {
  running (fakeApp) {
    // results for some various requests to login endpoints
    val getLoginResult             = FakeToyBox.getLogin(getLoginRequest)
    val failLoginWithoutInitResult = tryLogin(failPostLogin)
    val succLoginWithoutInitResult = tryLogin(succPostLogin)
    val failLoginWithInitResult    = tryLogin(withInit(failPostLogin))
    val succLoginWithInitResult    = tryLogin(withInit(succPostLogin))

    "A DefaultTBController" should "return a 200 OK with a valid HTML resource (such as login page) on GET" in {
      status(getLoginResult) should be (OK)
      contentType(getLoginResult) should be (Some("text/html"))
    }
    
    it should "redirect to the originally requested resource or app root on successful POST" in {
      redirectLocation(succLoginWithoutInitResult) should be (Some("/"))
      redirectLocation(succLoginWithInitResult) should be (Some(initCookiePath))
    }

    it should """add an "authenticated" cookie ONLY to successful POST responses""" in {
      // successful logins
      assert(resultSetsCookie(succLoginWithInitResult, FakeToyBox.authCookieName),
        """Successful login result does not set an "authenticated" cookie.""")
      assert(resultSetsCookie(succLoginWithoutInitResult, FakeToyBox.authCookieName),
        """Successful login result does not set an "authenticated" cookie (without saved initial request).""")

      // failed logins
      assert(!resultSetsCookie(failLoginWithInitResult, FakeToyBox.authCookieName),
        """Failed login result sets an "authenticated" cookie.""")
      assert(!resultSetsCookie(failLoginWithoutInitResult, FakeToyBox.authCookieName),
        """Failed login result sets an "authenticated" cookie (without saved initial request).""") 
    }
    
    it should "remain on the login page on failed POST" in {
      status(failLoginWithoutInitResult) should be (BAD_REQUEST)
      status(failLoginWithInitResult) should be (BAD_REQUEST)
    }
  }
}