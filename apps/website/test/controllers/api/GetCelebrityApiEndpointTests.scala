package controllers.api

import play.api.libs.json._
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.db.DBSession
import services.db.TransactionSerializable
import services.AppConfig
import services.Time
import utils.FunctionalTestUtils._
import utils.ClearsCacheBefore
import utils.EgraphsUnitTest
import utils.TestData
import models.JsCelebrity
import models.JsCelebrity._

class GetCelebrityApiEndpointTests 
  extends EgraphsUnitTest 
  with ClearsCacheBefore 
  with ProtectedCelebrityResourceTests
{
  override protected def routeUnderTest = controllers.routes.ApiControllers.getCelebrity
  private def db = AppConfig.instance[DBSession]

  routeName(routeUnderTest) should "get a Celebrity" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    // Execute the request
    val url = controllers.routes.ApiControllers.getCelebrity.url
    val req = FakeRequest(GET, url).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be (OK)

    val json = Json.parse(contentAsString(result))
    val celebrityFromJson = json.as[JsCelebrity]

    celebrityFromJson should be (JsCelebrity.from(celebrity))
  }
}
