package controllers.website

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult
import utils.FunctionalTestUtils._
import controllers.routes.WebsiteControllers.postSubscribeEmail
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests

class PostBulkEmailTests extends EgraphsUnitTest with CsrfProtectedResourceTests {
  override protected def routeUnderTest = postSubscribeEmail
  
  routeName(postSubscribeEmail) should "reject empty email addresses" in new EgraphsTestApplication {
    val result = performRequest(email="")
    
    status(result) should be (BAD_REQUEST)
    contentAsString(result) should be ("We're gonna need a valid email address")
  }

  it should "reject invalid email addresses" in new EgraphsTestApplication {
    val result = performRequest(email="derp@schlerp")
    
    status(result) should be (BAD_REQUEST)
    contentAsString(result) should be ("We're gonna need a valid email address")
  }

  it should "accept requests with a valid e-mail address" in new EgraphsTestApplication {
    val result = performRequest(email="customer@website.com")
    status(result) should be (200)
    contentAsString(result) should be ("subscribed")
  }
  
  //
  // Private members
  //
  private def performRequest(email: String): play.api.mvc.Result = {
    controllers.WebsiteControllers.postSubscribeEmail(
      FakeRequest().withFormUrlEncodedBody("email" -> email).withAuthToken
    )
  }
}