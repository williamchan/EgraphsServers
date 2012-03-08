package controllers.api

import services.blobs.Blobs
import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.TestConstants
import utils.{DBTransactionPerTest, EgraphsUnitTest}
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}
import services.AppConfig
import services.http.HttpCodes

class PostEgraphApiEndpointTests extends EgraphsUnitTest with DBTransactionPerTest {
  "PostEgraphApiEndpoint" should "save an egraph with status Verified if it skips biometrics" in {
    /*
    val response = new Controller with PostEgraphApiEndpoint {
      override implicit def validationErrors = Map.empty

      def celebFilters = friendlyFilters()
      def orderFilters = mock[OrderRequestFilters]
    }*/
    // TODO(erem): write this test
  }
}

class PostEgraphApiEndpointFunctionalTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  val blobs = AppConfig.instance[Blobs]

  @Test
  def testPostEgraph() {
    runWillChanScenariosThroughOrder()

    val ordersResponse = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    val ordersList = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse))

    val firstOrderMap = ordersList.head
    val orderId = firstOrderMap("id")

    val response = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/" + orderId + "/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + TestConstants.signatureStr + "&audio=" + TestConstants.voiceStrPercentEncoded() + "&skipBiometrics=1"
    )
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, Any]](getContent(response))
    assertEquals(BigDecimal(1), json("id"))

    import Blobs.Conversions._
    assertEquals(TestConstants.signatureStr, blobs.get("egraphs/" + json("id") + "/signature.json").get.asString)
  }

  @Test
  def testPostEgraphAcceptsMessage {
    runWillChanScenariosThroughOrder()

    val signatureStr = TestConstants.signatureStr
    val audioStr = TestConstants.voiceStrPercentEncoded()
    val message = TestConstants.messageStr

    val response = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "message=" + message + "&signature=" + signatureStr + "&audio=" + audioStr + "&skipBiometrics=1"
    )

    assertIsOk(response)
  }

  @Test
  def testPostEgraphAcceptsEmptyMessage {
    runWillChanScenariosThroughOrder()

    val signatureStr = TestConstants.signatureStr
    val audioStr = TestConstants.voiceStrPercentEncoded()
    val message = TestConstants.messageStr

    val emptyMessageResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "message=&signature=" + signatureStr + "&audio=" + audioStr + "&skipBiometrics=1"
    )

    val noMessageParamResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + signatureStr + "&audio=" + audioStr + "&skipBiometrics=1"
    )

    assertStatus(200, emptyMessageResponse)
    assertStatus(200, noMessageParamResponse)
  }

  @Test
  def testPostEgraphRejectsEmptySignatureAndAudio() {
    runWillChanScenariosThroughOrder()

    val emptyStringSignatureResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=&audio=" + TestConstants.voiceStrPercentEncoded() + "&skipBiometrics=1"
    )
    val noSignatureParameterResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "audio=" + TestConstants.voiceStrPercentEncoded() + "&skipBiometrics=1"
    )

    val emptyStringAudioResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + TestConstants.signatureStr + "&audio=&skipBiometrics=1"
    )

    val noAudioParameterResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + TestConstants.signatureStr + "&skipBiometrics=1"
    )


    assertStatus(HttpCodes.MalformedEgraph, emptyStringSignatureResponse)
    assertStatus(HttpCodes.MalformedEgraph, noSignatureParameterResponse)
    assertStatus(HttpCodes.MalformedEgraph, emptyStringAudioResponse)
    assertStatus(HttpCodes.MalformedEgraph, noAudioParameterResponse)
  }

  @Test
  def testPostEgraphDrainsOrdersQueue() {
    runWillChanScenariosThroughOrder()

    var numOrders = 2
    val ordersResponse = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    val ordersList = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse))
    assertEquals(numOrders, ordersList.length)

    for (order <- ordersList) {
      val response = POST(
        willChanRequest,
        TestConstants.ApiRoot + "/celebrities/me/orders/" + order("id") + "/egraphs",
        APPLICATION_X_WWW_FORM_URLENCODED,
        "signature=" + TestConstants.signatureStr + "&audio=" + TestConstants.voiceStr + "&skipBiometrics=1"
      )
      assertIsOk(response)
      numOrders -= 1
      val ordersResponse1 = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
      val ordersList1 = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse1))
      assertEquals(numOrders, ordersList1.length)
    }
  }

  private def runWillChanScenariosThroughOrder() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )
  }
}
