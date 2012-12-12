package controllers.api

import play.api.mvc.Request
import play.api.mvc.Controller
import play.api.test.Helpers._
import services.Time
import sjson.json.Serializer
import utils.FunctionalTestUtils.{runFreshScenarios, requestWithCredentials, routeName}
import models._
import enums.EnrollmentStatus
import services.http.BasicAuth
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest, MockControllerMethod, TestConstants}
import play.api.test.FakeRequest
import controllers.routes.ApiControllers.getCelebrity
import services.db.DBSession
import services.AppConfig
import services.db.TransactionSerializable
import scenario.RepeatableScenarios

class GetCelebrityApiEndpointTests 
  extends EgraphsUnitTest 
  with ClearsCacheAndBlobsAndValidationBefore 
  with ProtectedCelebrityResourceTests
{
  override protected def routeUnderTest = getCelebrity
  private def db = AppConfig.instance[DBSession]

  routeName(routeUnderTest) should "get a Celebrity" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = RepeatableScenarios.createCelebrity(isFeatured = true)
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
