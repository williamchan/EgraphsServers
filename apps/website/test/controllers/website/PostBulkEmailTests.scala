package controllers.website


import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.postSubscribeEmail
import sjson.json.Serializer
import utils.EgraphsUnitTest


class PostBulkEmailTests extends EgraphsUnitTest {
  
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