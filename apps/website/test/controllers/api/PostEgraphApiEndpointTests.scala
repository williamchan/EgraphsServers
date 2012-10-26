package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{
  willChanRequest, 
  runFreshScenarios, 
  routeName, 
  runWillChanScenariosThroughOrder
}
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import utils.TestConstants
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.{postEgraph, getCelebrityOrders}
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.blobs.Blobs
import services.{Time, AppConfig}
import services.http.HttpCodes
import services.db.{DBSession, TransactionSerializable}
import Blobs.Conversions._
import models.EgraphStore

class PostEgraphApiEndpointTests extends EgraphsUnitTest with ProtectedCelebrityResourceTests {
  private def blobs = AppConfig.instance[Blobs]
  private def db = AppConfig.instance[DBSession]
  private def egraphStore = AppConfig.instance[EgraphStore]

  protected override def routeUnderTest = postEgraph(1L)
  protected override def validRequestBodyAndQueryString = {
    // TODO: Once we're on Play 2.1 then get rid of this necessary indirection to satisfy
    //   invariance of FakeRequest[A], which should be FakeRequest[+A]    
    val formRequest = FakeRequest().withFormUrlEncodedBody(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "latitude" -> "37.7821120598956",
      "longitude" -> "-122.400612831116",
      "signedAt" -> "2012-07-12 15:11:22.987"
    )

    val anyContentRequest = formRequest.copy(body=(formRequest.body: AnyContent))

    Some(anyContentRequest)
  }


  routeName(postEgraph(1L)) should "accept a well-formed egraph as its first submission" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()
    
    val ordersList = getWillChanOrdersJson

    val firstOrderMap = ordersList.head
    val orderId = firstOrderMap("id")

    val (code, Some(egraphId)) = performEgraphPost()(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "latitude" -> "37.7821120598956",
      "longitude" -> "-122.400612831116",
      "signedAt" -> "2012-07-12 15:11:22.987"
    )
    
    code should be (OK)

    egraphId should be (1L)

    val signatureBlob = blobs.get("egraphs/" + egraphId + "/signature.json").get.asString
    signatureBlob should be (TestConstants.shortWritingStr)

    db.connected(TransactionSerializable) {
      val egraph = egraphStore.get(egraphId)
      egraph.latitude should be (Some(37.7821120598956))
      egraph.longitude should be (Some(-122.400612831116))
      egraph.signedAt should be (Time.timestamp("2012-07-12 15:11:22.987", Time.ipadDateFormat))
      egraph.assets.signature should be (TestConstants.shortWritingStr)
      egraph.assets.message should be (None)
    }
  }

  it should "accept an optional written message in addition to the signature" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()

    val signatureStr = TestConstants.shortWritingStr
    val audioStr = TestConstants.voiceStr_8khz
    val message = TestConstants.shortWritingStr

    val (code, Some(egraphId)) = performEgraphPost()(
      "message" -> message, 
      "signature" -> signatureStr, 
      "audio" -> audioStr,
      "signedAt" -> "2012-07-12 15:11:22.987"
    )

    code should be (OK)
    
    db.connected(TransactionSerializable) {
      val egraph = egraphStore.get(egraphId)
      egraph.assets.message should be (Some(TestConstants.shortWritingStr))
    }
  }

  it should "accept an empty message parameter" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()

    val signatureStr = TestConstants.shortWritingStr
    val audioStr = TestConstants.voiceStr_8khz()

    val (code, Some(egraphId)) = performEgraphPost()(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> "2012-07-12 15:11:22.987",
      "message" -> ""
    )

    code should be (OK)

    db.connected(TransactionSerializable) {
      egraphStore.get(egraphId).assets.message should be (None)
    }
  }

  it should "reject empty signature and audio" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()

    val emptyStringSignatureResponse = performEgraphPost()(
      "signature" -> "",
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> "2012-07-12 15:11:22.987"
    )
    val noSignatureParameterResponse = performEgraphPost()(
      "audio" -> TestConstants.voiceStr_8khz(),
      "signedAt" -> "2012-07-12 15:11:22.987"
    )

    val emptyStringAudioResponse = performEgraphPost()(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> "",
      "signedAt" -> "2012-07-12 15:11:22.987"
    )

    val noAudioParameterResponse = performEgraphPost()(
      "signature" -> TestConstants.shortWritingStr,
      "signedAt" -> "2012-07-12 15:11:22.987"
    )

    val malformedEgraphResult = (HttpCodes.MalformedEgraph, None)

    emptyStringSignatureResponse should be (malformedEgraphResult)
    noSignatureParameterResponse should be (malformedEgraphResult)
    emptyStringAudioResponse should be (malformedEgraphResult)
    noAudioParameterResponse should be (malformedEgraphResult)
  }

  it should "drain the orders queue when an order is fulfilled with a valid egraph" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()

    val numOrdersMade = 2
    
    for ((order, i) <- getWillChanOrdersJson.zipWithIndex) {
      getWillChanOrdersJson.length should be (numOrdersMade - i)
      
      val (code, Some(egraphId)) = performEgraphPost(order("id").toString.toLong)(
        "signature" -> TestConstants.shortWritingStr,
        "audio" -> TestConstants.voiceStr_8khz(),
        "signedAt" -> "2012-07-12 15:11:22.987"
      )
      code should be (OK)
    }

    getWillChanOrdersJson.length should be (0)
  }

  /** Gets the list of will chan orders and returns their json map */
  private def getWillChanOrdersJson:List[Map[String, Any]] = {
    val Some(ordersResult) = routeAndCall(willChanRequest.copy(GET, getCelebrityOrders(Some(true)).url))
    status(ordersResult) should be (OK)
    Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(ordersResult))
  }

  /** Posts the egraph and returns the result status on the left and egraphId on the right */
  private def performEgraphPost(orderId: Long=1L)(body: (String, String)*): (Int, Option[Long]) = {
    val Some(result) = routeAndCall(
      willChanRequest.copy(POST, postEgraph(orderId).url).withFormUrlEncodedBody(body:_*)
    )

    val code = status(result)
    val maybeEgraphId = if (code == OK) {
      val json = Serializer.SJSON.in[Map[String, Any]](contentAsString(result))
      Some(json("id").toString.toLong)
    } else {
      None
    }
    
    (code, maybeEgraphId)
  }
}
