package utils

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils._
import play.api.mvc.Call
import services.mvc.MultipartFormTestHelper

trait CsrfProtectedResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  
  routeName(routeUnderTest) + ", as a CSRF-safe resource, " should "fail due to lack of auth token when the request lacks one" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest().toCall(routeUnderTest))

    status(result) should not be (OK)
    contentAsString(result) should include ("token")
  }
  
  it should "not throw an error due to authenticity tokens when the correct one is provided" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest().toCall(routeUnderTest).withAuthToken)

    contentAsString(result) should not include ("token")
  }
}

/**
 * This trait works similarly to CsrfProtectedResourceTests, except is designed to test multipart form submissions
 */
trait CsrfProtectedMultipartFormResourceTests extends MultipartFormTestHelper { this: EgraphsUnitTest =>

  routeName(routeUnderTest) + ", as a CSRF-safe resource, " should "fail due to lack of auth token when the request lacks one" in new EgraphsTestApplication {
    val result = controllerMethod(request)

    status(result) should not be (OK)

    contentAsString(result) should include ("token")
  }

  it should "not throw an error due to authenticity tokens when the correct one is provided" in new EgraphsTestApplication {
    val result = controllerMethod(request.withAuthToken)

    contentAsString(result) should not include ("token")
  }

}
