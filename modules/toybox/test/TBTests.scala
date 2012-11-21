package egraphs.toybox.tests

import egraphs.toybox.tests.TBMocks._
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

/** Tests implementation of the methods of the ToyBoxBase trait */
@RunWith(classOf[JUnitRunner])
class ToyBoxTests extends FlatSpec with ShouldMatchers {
  running (fakeApp) {
    // run some requests through the mock ToyBox's onRouteRequest
    val unauthResult = routeRequestToSimpleResult(blankGetRequest)
    val authResult   = routeRequestToSimpleResult(authenticated(blankGetRequest))

    "A ToyBox" should "redirect unauthenticated requests to login" in {
      // verify that unauthenticated request was redirected to log-in
      redirectLocation(unauthResult) match {
        case Some(loc: String) =>  
          assert(loc === FakeToyBox.getLoginRoute.url, 
            "Request was redirected, but not to login.")
        case None => 
          fail("Request was not redirected at all.")
      }
    }

    it should "store initial request URL in designated cookie (only for unauthorized requests)" in {
      // check initial request cookie is set on unauthorized request
      assert(resultSetsCookie(unauthResult, FakeToyBox.initialRequestCookieName),
        "Result for unauthorized initial request doesn't set the initial request cookie.")

      // check initial request cookie is NOT set on authorized request
      assert(!resultSetsCookie(authResult, FakeToyBox.initialRequestCookieName),
        "Result for authorized initial request sets the initial request cookie.")
    }

    it should "not block requests for the login page or its assets" in {
      // get login page
      val pageResult = routeRequestToSimpleResult(getLoginRequest)
      assert(status(pageResult) != SEE_OTHER, "Login page request was blocked.")
      
      // get login asset
      val assetResult = routeRequestToSimpleResult(getLoginAssetRequest)
      assert(status(assetResult) != SEE_OTHER, "Login asset request was blocked.")
    }

    it should "pass authenticated requests" in {
      assert(status(authResult) === OK, 
        "Authenticated request received a " + status(authResult))
    }

    it should "renew authentication cookie for authenticated requests" in {
      assert(resultSetsCookie(authResult, FakeToyBox.authCookieName),
        "Result for authenticated request does not renew cookie.")
    }
  }
}

/** Tests the implementation of the methods of the ToyBoxController trait */
@RunWith(classOf[JUnitRunner])
class ToyBoxControllerTests extends FlatSpec with ShouldMatchers {
  running (fakeApp) {
    // results for some various requests to login endpoints
    val getLoginResult             = FakeToyBox.getLogin(getLoginRequest)
    val failLoginWithoutInitResult = tryLogin(failPostLogin)
    val succLoginWithoutInitResult = tryLogin(succPostLogin)
    val failLoginWithInitResult    = tryLogin(withInit(failPostLogin))
    val succLoginWithInitResult    = tryLogin(withInit(succPostLogin))

    "A ToyBoxController" should "return a 200 OK with a valid HTML resource (such as login page) on GET" in {
      assert(status(getLoginResult) === OK, 
        "Login page request should receive an OK. Received " + status(getLoginResult) + ".")
      
      contentType(getLoginResult) match {
        case Some(contentType: String) => 
          assert(contentType.startsWith("text/html"), "Response returning unexpected resource type.")
        case _ => 
          fail("Content-type not set")
      }
    }
    
    it should "redirect to the originally requested resource or app root on successful POST" in {
      redirectLocation(succLoginWithoutInitResult) match {
        case Some(loc: String) => 
          assert(loc === "/", "Successful login without saved initial request not redirect to root.")
        case _ => 
          fail("Successful login without saved initial request wasn't redirected.")
      }

      redirectLocation(succLoginWithInitResult) match {
        case Some(loc: String) => 
          assert(loc === initCookiePath, "Successful login not redirected to initial request path.")
        case _ => 
          fail("Successful login with saved initial request wasn't redirected.")
      }
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
      assert(status(failLoginWithoutInitResult) === BAD_REQUEST)
      assert(status(failLoginWithInitResult)    === BAD_REQUEST)
    }
  }
}