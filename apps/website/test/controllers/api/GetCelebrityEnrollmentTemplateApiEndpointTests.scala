package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{requestWithCredentials, routeName}
import utils.TestConstants
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import scenario.RepeatableScenarios
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
      val celebrity = RepeatableScenarios.createCelebrity()
      (celebrity, celebrity.account)
    }

    val url = controllers.routes.ApiControllers.getCelebrityEnrollmentTemplate.url
    val Some(result) = routeAndCall(
      requestWithCredentials(celebrityAccount).copy(GET, url)
    )
    
    status(result) should be (OK)

    val json = Serializer.SJSON.in[Map[String, List[Map[String, String]]]](contentAsString(result))
    json.size should be (1)
    
    val enrollmentPhrases: List[Map[String, String]] = json(_enrollmentPhrases)
    
    enrollmentPhrases.size should be (EnrollmentBatch.batchSize)
    "My name is " + celebrity.publicName should be (enrollmentPhrases(0)(_text))
    "One, two, three, four, five" should be (enrollmentPhrases(1)(_text))
    "Stop each car if it's little" should be (enrollmentPhrases(2)(_text))
    "Play in the street up ahead" should be (enrollmentPhrases(3)(_text))
    "A fifth wheel caught speeding" should be (enrollmentPhrases(4)(_text))
    "It's been about two years since Davey kept shotguns" should be (enrollmentPhrases(5)(_text))
    "Charlie did you think to measure the tree" should be (enrollmentPhrases(6)(_text))
    "Tina got cued to make a quicker escape" should be (enrollmentPhrases(7)(_text))
    "Joe books very few judges" should be (enrollmentPhrases(8)(_text))
    "Here I was in Miami and Illinois" should be (enrollmentPhrases(9)(_text))
    "Stop each car if it's little" should be (enrollmentPhrases(10)(_text))
    "Play in the street up ahead" should be (enrollmentPhrases(11)(_text))
    "A fifth wheel caught speeding" should be (enrollmentPhrases(12)(_text))
    "It's been about two years since Davey kept shotguns" should be (enrollmentPhrases(13)(_text))
    "Charlie did you think to measure the tree" should be (enrollmentPhrases(14)(_text))
    "Tina got cued to make a quicker escape" should be (enrollmentPhrases(15)(_text))
    "Joe books very few judges" should be (enrollmentPhrases(16)(_text))
    "Here I was in Miami and Illinois" should be (enrollmentPhrases(17)(_text))
    "Six, seven, eight, nine, ten" should be (enrollmentPhrases(18)(_text))
    "My name is " + celebrity.publicName should be (enrollmentPhrases(19)(_text))
  }
}
