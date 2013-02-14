package controllers.api

import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.libs.json._
import utils.TestConstants
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.blobs.Blobs
import services.{Time, AppConfig}
import services.http.HttpCodes
import services.db.{DBSession, TransactionSerializable}
import Blobs.Conversions._
import models.EgraphStore
import utils.TestData
import models.Account
import models.Celebrity
import utils.FunctionalTestUtils._

class PostEgraphApiEndpointTests extends EgraphsUnitTest with ProtectedCelebrityResourceTests {
  private def blobs = AppConfig.instance[Blobs]
  private def db = AppConfig.instance[DBSession]
  private def egraphStore = AppConfig.instance[EgraphStore]

  private val latitude = 37.7821120598956
  private val longitude = -122.400612831116
  private val signedAt = "2012-07-12 15:11:22.987"

  protected override def routeUnderTest = controllers.routes.ApiControllers.postEgraph(1L)
  protected override def validRequestBodyAndQueryString = {
    // TODO: Once we're on Play 2.1 then get rid of this necessary indirection to satisfy
    //   invariance of FakeRequest[A], which should be FakeRequest[+A]    
    val formRequest = newRouteUnderTestFakeRequest.withFormUrlEncodedBody(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "latitude" -> latitude.toString,
      "longitude" -> longitude.toString,
      "signedAt" -> signedAt
    )

    val anyContentRequest = formRequest.withBody(formRequest.body)

    Some(anyContentRequest)
  }

  "postEgraph" should "accept a well-formed egraph as its first submission" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders)
    }

    val order = orders.head // we only need 1 order for this test, so let's use the first one

    val (code, Some(egraphId)) = performEgraphPost(celebrityAccount, order.id)(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "latitude" -> latitude.toString,
      "longitude" -> longitude.toString,
      "signedAt" -> signedAt
    )
    
    code should be (OK)

    val egraph = db.connected(TransactionSerializable) {
      egraphStore.findByOrder(order.id).single
    }
    egraphId should be (egraph.id)

    val signatureBlob = blobs.get("egraphs/" + egraphId + "/signature.json").get.asString
    signatureBlob should be (TestConstants.shortWritingStr)

    db.connected(TransactionSerializable) {
      val foundEgraph = egraphStore.get(egraphId)
      foundEgraph.latitude should be (Some(latitude))
      foundEgraph.longitude should be (Some(longitude))
      foundEgraph.signedAt should be (Time.timestamp(signedAt, Time.ipadDateFormat))
      foundEgraph.assets.signature should be (TestConstants.shortWritingStr)
      foundEgraph.assets.message should be (None)
    }
  }

  it should "accept an optional written message in addition to the signature" in new EgraphsTestApplication {
    val (celebrityAccount, order) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders.head)
    }

    val message = TestConstants.shortWritingStr

    val (code, Some(egraphId)) = performEgraphPost(celebrityAccount, order.id)(
      "message" -> message,
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> signedAt
    )

    code should be (OK)
    
    db.connected(TransactionSerializable) {
      val egraph = egraphStore.get(egraphId)
      egraph.assets.message should be (Some(message))
    }
  }

  it should "accept an empty message parameter" in new EgraphsTestApplication {
    val (celebrityAccount, order) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders.head)
    }

    val (code, Some(egraphId)) = performEgraphPost(celebrityAccount, order.id)(
      "message" -> "",
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> signedAt
    )

    code should be (OK)

    db.connected(TransactionSerializable) {
      egraphStore.get(egraphId).assets.message should be (None)
    }
  }

  it should "reject empty signature and audio" in new EgraphsTestApplication {
    val (celebrityAccount, order) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders.head)
    }

    val emptyStringSignatureResponse = performEgraphPost(celebrityAccount, order.id)(
      "signature" -> "",
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> signedAt
    )

    val noSignatureParameterResponse = performEgraphPost(celebrityAccount, order.id)(
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> signedAt
    )

    val emptyStringAudioResponse = performEgraphPost(celebrityAccount, order.id)(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> "",
      "signedAt" -> signedAt
    )

    val noAudioParameterResponse = performEgraphPost(celebrityAccount, order.id)(
      "signature" -> TestConstants.shortWritingStr,
      "signedAt" -> signedAt
    )

    val malformedEgraphResult = (HttpCodes.MalformedEgraph, None)

    emptyStringSignatureResponse should be (malformedEgraphResult)
    noSignatureParameterResponse should be (malformedEgraphResult)
    emptyStringAudioResponse should be (malformedEgraphResult)
    noAudioParameterResponse should be (malformedEgraphResult)
  }

  it should "drain the orders queue when an order is fulfilled with a valid egraph" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders)
    }

    for ((order, i) <- orders.zipWithIndex) {
      val json = getCelebrityOrders(celebrityAccount)
      val jsonOrders = json.as[JsArray].value
      jsonOrders.size should be (orders.size - i)

      val (code, Some(egraphId)) = performEgraphPost(celebrityAccount, order.id)(
        "signature" -> TestConstants.shortWritingStr,
        "audio" -> TestConstants.voiceStr_8khz(),
        "signedAt" -> "2012-07-12 15:11:22.987"
      )
      code should be (OK)
    }

    getCelebrityOrders(celebrityAccount).as[JsArray].value.size should be (0)
  }

  /** Gets the list of orders needed to be completed by the celebrity and returns their json map */
  private def getCelebrityOrders(celebrityAccount: Account): JsValue = {
    val url = controllers.routes.ApiControllers.getCelebrityOrders(Some(true)).url
    val Some(ordersResult) = route(
      FakeRequest(GET, url).withCredentials(celebrityAccount)
    )
    status(ordersResult) should be (OK)
    Json.parse(contentAsString(ordersResult))
  }

  /** Posts the egraph and returns the result status on the left and egraphId on the right */
  private def performEgraphPost(celebrityAccount: Account, orderId: Long)(body: (String, String)*): (Int, Option[Long]) = {
    val url = controllers.routes.ApiControllers.postEgraph(orderId).url
    val Some(result) = route(
      FakeRequest(POST, url).withCredentials(celebrityAccount).withFormUrlEncodedBody(body:_*)
    )

    val code = status(result)
    val maybeEgraphId = if (code == OK) {
      val json = Json.parse(contentAsString(result))
      Some((json \ "id").as[Long])
    } else {
      None
    }
    
    (code, maybeEgraphId)
  }
}
