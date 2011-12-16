package controllers.api

import libs.Blobs
import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.TestConstants
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}

class CelebrityOrderApiControllersTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  @Test
  def testPostEgraph() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

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

    // See CelebrityOrderApiControllers.postEgraph
    import Blobs.Conversions._
    assertEquals(TestConstants.signatureStr, Blobs.get("egraphs/" + json("id") + "/signature.json").get.asString)
  }

    @Test
    def testPostEgraphDrainsOrdersQueue() {
      runScenarios(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products",
        "Erem-is-a-customer",
        "Erem-buys-Wills-two-products-twice-each"
      )

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
}
