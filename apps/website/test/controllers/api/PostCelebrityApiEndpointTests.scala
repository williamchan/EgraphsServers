package controllers.api

import scala.util.Random
import org.apache.commons.lang3.RandomStringUtils
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

  "postCelebrityContactInfo" should "create a new celebrity contact info if none exists" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity(secureInfo = None)
      celebrity.secureInfoId should be(None)
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

  it should "update a celebrity contact info if one exists" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount, secureInfo) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity().copy(twitterUsername = Some(RandomStringUtils.randomAlphanumeric(10))).save
      (celebrity, celebrity.account, celebrity.secureInfo)
    }

    val newTwitterUsername = RandomStringUtils.randomAlphanumeric(10)
    val newContactInfo = secureInfo.get.copy(contactEmail = Some(TestData.generateEmail("clown", "face.com")))
    val requestJson = Json.toJson(JsCelebrityContactInfo.from(celebrity, Some(newContactInfo)).copy(twitterUsername = Some(newTwitterUsername)))

    // Execute the request
    val url = controllers.routes.ApiControllers.postCelebrityContactInfo.url
    val req = FakeRequest(POST, url).withJsonBody(requestJson).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be(OK)

    db.connected(TransactionSerializable) {
      val Some(savedCelebrity) = celebrityStore.findById(celebrity.id)
      savedCelebrity.twitterUsername should be (Some(newTwitterUsername))
      val maybeSavedContactInfo = savedCelebrity.secureInfo
      maybeSavedContactInfo.isDefined should be(true)
      val Some(savedContactInfo) = maybeSavedContactInfo
      savedContactInfo.contactEmail should be(newContactInfo.contactEmail)
      savedContactInfo.smsPhone should be(secureInfo.get.smsPhone)
    }
  }

  it should "return a bad request if an email has invalid formatting" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount, secureInfo) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account, celebrity.secureInfo)
    }

    val newContactInfo = secureInfo.get.copy(contactEmail = Some("invalid_email_address"))
    val requestJson = Json.toJson(JsCelebrityContactInfo.from(celebrity, Some(newContactInfo)))

    // Execute the request
    val url = controllers.routes.ApiControllers.postCelebrityContactInfo.url
    val req = FakeRequest(POST, url).withJsonBody(requestJson).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be(BAD_REQUEST)

    db.connected(TransactionSerializable) {
      val maybeSavedContactInfo = celebrityStore.findById(celebrity.id).get.secureInfo
      maybeSavedContactInfo.isDefined should be(true)
      val Some(savedContactInfo) = maybeSavedContactInfo
      // nothing should have changed in the db
      savedContactInfo.contactEmail should be(secureInfo.get.contactEmail)
    }
  }

  "postCelebrityDepositInfo" should "create a new celebrity deposit info if none exists" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity(secureInfo = None)
      celebrity.secureInfoId should be(None)
      (celebrity, celebrity.account)
    }

    val depositInfo = DecryptedCelebritySecureInfo(addressLine1 = Some(RandomStringUtils.randomAlphanumeric(30)))
    val requestJson = Json.toJson(JsCelebrityDepositInfo.from(celebrity, Some(depositInfo)).copy(isDepositAccountChange = Some(false)))

    // Execute the request
    val url = controllers.routes.ApiControllers.postCelebrityDepositInfo.url
    val req = FakeRequest(POST, url).withJsonBody(requestJson).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be(OK)

    db.connected(TransactionSerializable) {
      val maybeSavedDepositInfo = celebrityStore.findById(celebrity.id).get.secureInfo
      maybeSavedDepositInfo.isDefined should be(true)
      val Some(savedDepositInfo) = maybeSavedDepositInfo
      savedDepositInfo.addressLine1 should be(depositInfo.addressLine1)
    }
  }

  it should "update a celebrity deposit info if one exists" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount, secureInfo) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account, celebrity.secureInfo)
    }

    val newDepositInfo = secureInfo.get.copy(addressLine1 = Some(RandomStringUtils.randomAlphanumeric(30)))
    val requestJson = Json.toJson(JsCelebrityDepositInfo.from(celebrity, Some(newDepositInfo)).copy(isDepositAccountChange = Some(false)))
    // Execute the request
    val url = controllers.routes.ApiControllers.postCelebrityDepositInfo.url
    val req = FakeRequest(POST, url).withJsonBody(requestJson).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be(OK)

    db.connected(TransactionSerializable) {
      val maybeSavedDepositInfo = celebrityStore.findById(celebrity.id).get.secureInfo
      maybeSavedDepositInfo.isDefined should be(true)
      val Some(savedDepositInfo) = maybeSavedDepositInfo
      savedDepositInfo.addressLine1 should be(newDepositInfo.addressLine1)
      savedDepositInfo.depositAccountNumber should be(secureInfo.get.depositAccountNumber)
    }
  }

  it should "update a celebrity deposit acount info if it already exists" in new EgraphsTestApplication {
    // Set up the scenario
    val (celebrity, celebrityAccount, secureInfo) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account, celebrity.secureInfo)
    }

    val newDepositInfo = JsCelebrityDepositInfo.from(celebrity, Some(secureInfo.get)).copy(
      isDepositAccountChange = Some(true),
      depositAccountNumber = Some(Random.nextLong.abs)
    )
    val requestJson = Json.toJson(newDepositInfo)
    // Execute the request
    val url = controllers.routes.ApiControllers.postCelebrityDepositInfo.url
    val req = FakeRequest(POST, url).withJsonBody(requestJson).withCredentials(celebrityAccount)
    val Some(result) = route(req)

    // Test expectations
    status(result) should be(OK)

    db.connected(TransactionSerializable) {
      val maybeSavedDepositInfo = celebrityStore.findById(celebrity.id).get.secureInfo
      maybeSavedDepositInfo.isDefined should be(true)
      val Some(savedDepositInfo) = maybeSavedDepositInfo
      savedDepositInfo.depositAccountRoutingNumber should be(None) // this should have been unset
      savedDepositInfo.depositAccountNumber should be(newDepositInfo.depositAccountNumber) // this should have been updated
    }
  }
}