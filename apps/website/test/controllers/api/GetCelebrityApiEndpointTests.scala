package controllers.api

import play.api.test.Helpers._
import sjson.json.Serializer
import models._
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.test.Helpers._
import play.api.test.FakeRequest
import services.db.DBSession
import services.db.TransactionSerializable
import services.AppConfig
import services.Time
import utils.FunctionalTestUtils.requestWithCredentials
import utils.FunctionalTestUtils.routeName
import utils.ClearsCacheBefore
import utils.EgraphsUnitTest
import utils.TestData

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
    val req = requestWithCredentials(celebrityAccount).copy(method = GET, uri = url)
    val Some(result) = routeAndCall(req)

    // Test expectations
    status(result) should be (OK)

    val json = Serializer.SJSON.in[Map[String, AnyRef]](contentAsString(result))

    json("id") should be (celebrity.id)
    json("publicName") should be (celebrity.publicName)
    json("urlSlug") should be (celebrity.urlSlug)
    json("enrollmentStatus") should be (celebrity.enrollmentStatus.name)

    // These conversions will fail if they're not Longs
    Time.fromApiFormat(json("created").toString)
    Time.fromApiFormat(json("updated").toString)

    json.size should be (6)
  }
}
