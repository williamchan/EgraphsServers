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


@RunWith(classOf[JUnitRunner])
class TBPluginTests extends FlatSpec with ShouldMatchers {
  running (fakeApp) {
    val maybeUnauthResult = routeRequestToMaybeResult(unauthenticatedRequest)
    val maybeAuthResult   = routeRequestToMaybeResult(authenticatedRequest)

    "TBPlugin" should "redirect unauthenticated requests to login" in {
      val unauthResult = maybeUnauthResult.getOrElse(
        fail("Unauthenticated request failed instead of getting redirected to login.")
      )
      
      // verify that unauthenticated request was redirected to log-in
      redirectLocation(unauthResult) match {
        case Some(loc: String) => 
          assert(loc === FakeToyBox.getLoginRoute.url, "Request was redirected, but not to login.")
        case _ => 
          fail("Request was not redirected at all.")
      }
    }

    // TODO: check values of the cookie?? is it worth it?
    it should "store initial request URL in designated cookie" in {
      maybeUnauthResult match {
        case Some(simpleResult: SimpleResult[_]) =>
          assert(resultSetsCookie(simpleResult, FakeToyBox.initialRequestCookie))
        case _ => 
          fail("Unauthenticated request did not receive a SimpleResult")
      }
    }

    // ******************************************************************************* //
    // This test passes as of 11/15/12 when mocking the default handler, but I'm
    // currently not aware of a simple uniform way to do this (I tested by modifying
    // the source of ToyBoxPlugin)

    // Solutions: We could clutter the API by abstracting out super.onRouteRequest so
    // it's easily mockable, or I think a mock router needs to be set up.
    // ******************************************************************************* //

    it should "not block requests for the login page or its assets" in (pending) /*{
      // get login page
      routeRequestToMaybeResult(getLoginRequest) match {
        case Some(result: Result) =>
          assert(status(result) != SEE_OTHER, "Login page request was blocked.")
        case _ => fail("Login page request received no result.")
      }
      
      // get login asset
      routeRequestToMaybeResult(getLoginAssetRequest) match {
        case Some(result: Result) =>
          assert(status(result) != SEE_OTHER, "Login asset request was blocked.")
        case _ => fail("Login asset request received no result.")
      }
    }*/

    it should "pass authenticated requests" in (pending) /*{
      maybeAuthResult match {
        case Some(result: Result) => 
          assert(status(result) === OK, "Authenticated request received a " + status(result))
        case None => 
          fail("no result for authenticated request?!")
      }
    }*/

    it should "renew authentication cookie for authenticated requests" in (pending) /*{
      maybeAuthResult match {
        case Some(result: Result) =>
          assert(resultSetsCookie(result, FakeToyBox.authenticatedCookie),
            "Result for authenticated request does not renew cookie.")
        case _ => 
          fail("Unexpected result or handler for authenticated request.")
      }
    }*/
  }
}

class TBControllerTests extends FlatSpec with ShouldMatchers {
  running (fakeApp) {
    val getLoginResult             = FakeToyBox.tbController.getLogin(getLoginRequest)
    val failLoginWithoutInitResult = tryLogin(failLoginWithoutInitRequest)
    val succLoginWithoutInitResult = tryLogin(succLoginWithoutInitRequest)
    val failLoginWithInitResult    = tryLogin(failLoginWithInitRequest)
    val succLoginWithInitResult    = tryLogin(succLoginWithInitRequest)

    "TBController" should "return a 200 OK with an HTML resource on GET" in {
      assert(status(getLoginResult) === OK)
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
      assert(resultSetsCookie(succLoginWithInitResult, FakeToyBox.authenticatedCookie),
        """Successful login result does not set an "authenticated" cookie.""")
      assert(resultSetsCookie(succLoginWithoutInitResult, FakeToyBox.authenticatedCookie),
        """Successful login result does not set an "authenticated" cookie (without saved initial request).""")

      // failed logins
      assert(!resultSetsCookie(failLoginWithInitResult, FakeToyBox.authenticatedCookie),
        """Failed login result sets an "authenticated" cookie.""")
      assert(!resultSetsCookie(failLoginWithoutInitResult, FakeToyBox.authenticatedCookie),
        """Failed login result sets an "authenticated" cookie (without saved initial request).""") 
    }
    
    // TODO - test with form errors
    it should "remain on the login page on failed POST" in {
      assert(status(failLoginWithoutInitResult) === BAD_REQUEST)
      assert(status(failLoginWithInitResult)    === BAD_REQUEST)
    }

    // TODO: consider testing the errors of login failure?

    // Verified that you can set multiple cookies at once
    /*"cookies" should "be cookies" in {
      failLoginWithoutInitResult match {
        case res: PlainResult =>
          val resWithTwoCookies = res.withCookies(authCookie, initCookie)
          println("setting two cookies looks like: " + resWithTwoCookies.header.headers(
            play.api.http.HeaderNames.SET_COOKIE))
      }
    }*/
  }
}