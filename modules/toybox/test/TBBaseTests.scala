package egraphs.toybox.tests

import egraphs.toybox.tests.TBFakes._
import controllers._
import controllers.ToyBoxConfigKeys._

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
class DefaultTBBaseTests extends FlatSpec with ShouldMatchers {
  running (fakeApp) {
    // run some requests through the fake ToyBox's onRouteRequest
    val unauthResult = routeRequestToSimpleResult(blankGetRequest)
    val authResult   = routeRequestToSimpleResult(authenticated(blankGetRequest))

    "A DefaultTBBase" should "redirect unauthenticated requests to login" in {
      // verify that unauthenticated request was redirected to log-in
      redirectLocation(unauthResult) should be (Some(FakeToyBox.getLoginRoute.url))
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
      status(routeRequestToSimpleResult(getLoginRequest)) should be (OK)
      
      // get login asset
      status(routeRequestToSimpleResult(getLoginAssetRequest)) should be (OK)
    }

    it should "pass authenticated requests" in {
      status(authResult) should be (OK)
    }

    it should "renew authentication cookie for authenticated requests" in {
      assert(resultSetsCookie(authResult, FakeToyBox.authCookieName),
        "Result for authenticated request does not renew cookie.")
    }

    it should "allow requests that contain ipad header and secret" in {
      status(routeRequestToSimpleResult(iPadRequest)) should be (OK)
    }
  }
}

