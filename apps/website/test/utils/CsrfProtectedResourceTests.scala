package utils

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import play.api.mvc.Controller
import org.scalatest.FlatSpec
import play.api.mvc.Call

trait CsrfProtectedResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  
  routeName(routeUnderTest) + ", as a CSRF-safe resource, " should "fail due to lack of auth token when the request lacks one" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(routeUnderTest))
    
    status(result) should not be (OK)
    contentAsString(result) should include ("token")
  }
  
  it should "not throw an error due to authenticity tokens when the correct one is provided" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(routeUnderTest).withAuthToken)
    
    contentAsString(result) should not include ("token")
  }
}