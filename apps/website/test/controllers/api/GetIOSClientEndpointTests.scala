package controllers.api

import play.api.mvc.Request
import play.api.mvc.Controller
import play.api.test.Helpers._
import services.Time
import utils.FunctionalTestUtils.routeName
import models._
import enums.EnrollmentStatus
import services.http.BasicAuth
import utils.{ClearsCacheBefore, EgraphsUnitTest, MockControllerMethod, TestConstants}
import play.api.test.FakeRequest
import controllers.routes.ApiControllers.getIOSClient
import utils.FunctionalTestUtils
import play.api.mvc.ChunkedResult
import FunctionalTestUtils.chunkedContent

class GetIOSClientEndpointTests extends EgraphsUnitTest {
  def routeUnderTest = getIOSClient(redirectToItmsLink=true)

  routeName(getIOSClient(redirectToItmsLink=true)) should "return a redirect to an itms-services link if queried with ?redirectToItmsLink=true" in new EgraphsTestApplication {
    // Execute the request
    val Some(result) = route(FakeRequest(GET, getIOSClient(redirectToItmsLink=true).url))

    // Test expectations
    status(result) should be (SEE_OTHER)
    redirectLocation(result).get should include ("itms-services://?action=download-manifest") 
    redirectLocation(result).get should include (getIOSClient(redirectToItmsLink=false).url) 
  }
  
  routeName(getIOSClient(redirectToItmsLink=false)) should "return the application plist if not queried with ?redirectToItmsLink=true" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest(GET, getIOSClient(redirectToItmsLink=false).url))
    
    status(result) should be (OK)
    contentType(result) should be (Some("application/octet-stream"))

    val plistAsString = new String(chunkedContent(result).toArray)
    
    plistAsString should include ("!DOCTYPE plist PUBLIC")
  }
}
