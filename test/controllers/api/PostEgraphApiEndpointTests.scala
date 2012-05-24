package controllers.api

import services.blobs.Blobs
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest}
import services.AppConfig
import services.http.HttpCodes
import utils.{FunctionalTestUtils, TestConstants}
import services.db.{TransactionSerializable, DBSession}
import org.junit.Assert._
import Blobs.Conversions._
import models.EgraphStore

class PostEgraphApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._
  private val db = AppConfig.instance[DBSession]
  private val blobs = AppConfig.instance[Blobs]
  private val egraphStore = AppConfig.instance[EgraphStore]

  @Test
  def testPostEgraph() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    val ordersResponse = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    val ordersList = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse))

    val firstOrderMap = ordersList.head
    val orderId = firstOrderMap("id")

    val response = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/" + orderId + "/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + TestConstants.shortWritingStr + "&audio=" + TestConstants.fakeAudioStrPercentEncoded() + "&latitude=37.7821120598956&longitude=-122.400612831116"
    )
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, Any]](getContent(response))
    assertEquals(BigDecimal(1), json("id"))
    assertEquals(TestConstants.shortWritingStr, blobs.get("egraphs/" + json("id") + "/signature.json").get.asString)

    db.connected(TransactionSerializable) {
      val egraph = egraphStore.findById(json("id").toString.toLong).get
      assertEquals(Some(37.7821120598956), egraph.latitude)
      assertEquals(Some(-122.400612831116), egraph.longitude)
    }
  }

  @Test
  def testPostEgraphAcceptsMessage() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    val signatureStr = TestConstants.shortWritingStr
    val audioStr = TestConstants.fakeAudioStrPercentEncoded()
    val message = TestConstants.shortWritingStr

    val response = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "message=" + message + "&signature=" + signatureStr + "&audio=" + audioStr
    )

    assertIsOk(response)
  }

  @Test
  def testPostEgraphAcceptsEmptyMessage() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    val signatureStr = TestConstants.shortWritingStr
    val audioStr = TestConstants.fakeAudioStrPercentEncoded()

    val emptyMessageResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "message=&signature=" + signatureStr + "&audio=" + audioStr
    )

    val noMessageParamResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + signatureStr + "&audio=" + audioStr
    )

    assertStatus(200, emptyMessageResponse)
    assertStatus(200, noMessageParamResponse)
  }

  @Test
  def testPostEgraphRejectsEmptySignatureAndAudio() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    val emptyStringSignatureResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=&audio=" + TestConstants.fakeAudioStrPercentEncoded()
    )
    val noSignatureParameterResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "audio=" + TestConstants.fakeAudioStrPercentEncoded()
    )

    val emptyStringAudioResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + TestConstants.shortWritingStr + "&audio="
    )

    val noAudioParameterResponse = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/1/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + TestConstants.shortWritingStr
    )

    assertStatus(HttpCodes.MalformedEgraph, emptyStringSignatureResponse)
    assertStatus(HttpCodes.MalformedEgraph, noSignatureParameterResponse)
    assertStatus(HttpCodes.MalformedEgraph, emptyStringAudioResponse)
    assertStatus(HttpCodes.MalformedEgraph, noAudioParameterResponse)
  }

  @Test
  def testPostEgraphDrainsOrdersQueue() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    var numOrders = 2
    val ordersResponse = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    val ordersList = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse))
    assertEquals(numOrders, ordersList.length)

    for (order <- ordersList) {
      val response = POST(
        willChanRequest,
        TestConstants.ApiRoot + "/celebrities/me/orders/" + order("id") + "/egraphs",
        APPLICATION_X_WWW_FORM_URLENCODED,
        "signature=" + TestConstants.shortWritingStr + "&audio=" + TestConstants.fakeAudioStrPercentEncoded()
      )
      assertIsOk(response)
      numOrders -= 1
      val ordersResponse1 = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
      val ordersList1 = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse1))
      assertEquals(numOrders, ordersList1.length)
    }
  }
}
