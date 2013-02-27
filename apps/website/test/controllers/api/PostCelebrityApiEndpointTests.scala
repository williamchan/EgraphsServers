package controllers.api

import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import utils.FunctionalTestUtils._
import utils._
import utils.FunctionalTestUtils.EgraphsFakeRequest
import models._
import enums.OrderReviewStatus
import services.db.TransactionSerializable
import services.AppConfig
import services.db.DBSession

class PostCelebrityApiEndpointTests
  extends EgraphsUnitTest
  with ProtectedCelebrityResourceTests
{
  protected override def routeUnderTest = controllers.routes.ApiControllers.postCelebrityContactInfo
  private def db = AppConfig.instance[DBSession]
  private def celebrityStore = AppConfig.instance[CelebrityStore]

  routeName(routeUnderTest) should "create a new celebrity contact info" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    val contactInfo = DecryptedCelebritySecureInfo(contactEmail = Some(TestData.generateEmail("clown", "face.com")))
    val requestJson = Json.toJson(JsCelebrityContactInfo.from(celebrity, Some(contactInfo)))

    // Execute the request
    val url = controllers.routes.ApiControllers.postCelebrityContactInfo.url
    val req = FakeRequest(POST, url).withJsonBody(requestJson).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be(OK)

    db.connected(TransactionSerializable) {
      val maybeSavedContactInfo = celebrityStore.findById(celebrity.id).get.secureInfo
      maybeSavedContactInfo.isDefined should be(true)
      val Some(savedContactInfo) = maybeSavedContactInfo
      savedContactInfo.contactEmail should be(contactInfo.contactEmail)
    }
  }
}