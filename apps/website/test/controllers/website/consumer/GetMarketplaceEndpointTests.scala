package controllers.website.consumer

import controllers.routes.WebsiteControllers.getMarketplaceResultPage
import controllers.WebsiteControllers
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import services.http.EgraphsSession
import play.api.test.Helpers._
import play.api.test._
import play.api.mvc.Result

class GetMarketplaceEndpointTests extends EgraphsUnitTest {
  private def db = AppConfig.instance[DBSession]
  def routeUnderTest = getMarketplaceResultPage("")
    
  "getMarketplaceResultPage" should "serve a default page" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest(GET, routeUnderTest.url))
    status(result) should be(OK)
  }

  "marketplace regex" should "not match invalid strings" in new EgraphsTestApplication {
    val invalidString  = "derp"
    val invalidString1  = "24"
    val invalidString2  = "c"
    WebsiteControllers.categoryRegex.findFirstIn(invalidString) should be(None)
    WebsiteControllers.categoryRegex.findFirstIn(invalidString1) should be(None)
    WebsiteControllers.categoryRegex.findFirstIn(invalidString2) should be(None)
  }

  it should "match strings of valid form" in new EgraphsTestApplication {
    val validString = "c8939930"
    WebsiteControllers.categoryRegex.findFirstIn(validString) should not be(None)  
  }
}