package controllers.api

import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import utils.FunctionalTestUtils._
import utils.TestConstants
import utils.TestData
import models.EnrollmentBatch
import services.AppConfig
import services.db.DBSession
import services.db.TransactionSerializable

class GetCelebrityEnrollmentTemplateApiEndpointTests 
  extends EgraphsUnitTest
  with ProtectedCelebrityResourceTests
{
  private def db = AppConfig.instance[DBSession]
  protected override def routeUnderTest = controllers.routes.ApiControllers.getCelebrityEnrollmentTemplate

  routeName(routeUnderTest) should "return the correct enrollment json" in {
    val _enrollmentPhrases: String = GetCelebrityEnrollmentTemplateApiEndpoint._enrollmentPhrases
    val _text: String = GetCelebrityEnrollmentTemplateApiEndpoint._text

    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    val url = controllers.routes.ApiControllers.getCelebrityEnrollmentTemplate.url
    val Some(result) = route(
      FakeRequest(GET, url).withCredentials(celebrityAccount)
    )
    
    status(result) should be (OK)

    val json = Json.parse(contentAsString(result)).as[JsObject]
    json.fields.size should be (1)

    val enrollmentPhrases = (json \ _enrollmentPhrases).as[JsArray].value
    
    enrollmentPhrases.size should be (EnrollmentBatch.batchSize)
    // these are all reversed from the order they should be acutal/expected
    "My name is " + celebrity.publicName should be ((enrollmentPhrases(0) \ _text).as[String])
    "One, two, three, four, five" should be ((enrollmentPhrases(1) \ _text).as[String])
    "Stop each car if it's little" should be ((enrollmentPhrases(2) \ _text).as[String])
    "Play in the street up ahead" should be ((enrollmentPhrases(3) \ _text).as[String])
    "A fifth wheel caught speeding" should be ((enrollmentPhrases(4) \ _text).as[String])
    "It's been about two years since Davey kept shotguns" should be ((enrollmentPhrases(5) \ _text).as[String])
    "Charlie did you think to measure the tree" should be ((enrollmentPhrases(6) \ _text).as[String])
    "Tina got cued to make a quicker escape" should be ((enrollmentPhrases(7) \ _text).as[String])
    "Joe books very few judges" should be ((enrollmentPhrases(8) \ _text).as[String])
    "Here I was in Miami and Illinois" should be ((enrollmentPhrases(9) \ _text).as[String])
    "Stop each car if it's little" should be ((enrollmentPhrases(10) \ _text).as[String])
    "Play in the street up ahead" should be ((enrollmentPhrases(11) \ _text).as[String])
    "A fifth wheel caught speeding" should be ((enrollmentPhrases(12) \ _text).as[String])
    "It's been about two years since Davey kept shotguns" should be ((enrollmentPhrases(13) \ _text).as[String])
    "Charlie did you think to measure the tree" should be ((enrollmentPhrases(14) \ _text).as[String])
    "Tina got cued to make a quicker escape" should be ((enrollmentPhrases(15) \ _text).as[String])
    "Joe books very few judges" should be ((enrollmentPhrases(16) \ _text).as[String])
    "Here I was in Miami and Illinois" should be ((enrollmentPhrases(17) \ _text).as[String])
    "Six, seven, eight, nine, ten" should be ((enrollmentPhrases(18) \ _text).as[String])
    "My name is " + celebrity.publicName should be ((enrollmentPhrases(19) \ _text).as[String])
  }
}
