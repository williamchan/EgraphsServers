package controllers.api

import play.api.mvc.Request
import play.api.mvc.Controller
import play.api.test.Helpers._
import services.Time
import sjson.json.Serializer
import utils.FunctionalTestUtils.{runFreshScenarios, willChanRequest, routeName}
import models._
import enums.EnrollmentStatus
import services.http.BasicAuth
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest, MockControllerMethod, TestConstants}
import play.api.test.FakeRequest
import controllers.routes.ApiControllers.getCelebrity

class GetCelebrityApiEndpointTests 
  extends EgraphsUnitTest 
  with ClearsCacheAndBlobsAndValidationBefore 
  with ProtectedCelebrityResourceTests
{
  override protected def routeUnderTest = getCelebrity

  routeName(routeUnderTest) should "get a Celebrity" in new EgraphsTestApplication {
    // Set up the scenario
    runFreshScenarios("Will-Chan-is-a-celebrity")

    // Execute the request
    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=TestConstants.ApiRoot + "/celebrities/me"))

    // Test expectations
    status(result) should be (OK)

    val json = Serializer.SJSON.in[Map[String, AnyRef]](contentAsString(result))

    json("id") should not be (null)
    json("publicName") should be ("Wizzle")
    json("urlSlug") should be ("Wizzle")
    json("enrollmentStatus") should be (EnrollmentStatus.NotEnrolled.name)

    // These conversions will fail if they're not Longs
    Time.fromApiFormat(json("created").toString)
    Time.fromApiFormat(json("updated").toString)

    json.size should be (6)
  }
}
