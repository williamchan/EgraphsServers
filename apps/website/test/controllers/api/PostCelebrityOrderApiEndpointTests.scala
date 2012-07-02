package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.willChanRequest
import models._
import enums.OrderReviewStatus
import utils.{FunctionalTestUtils, TestConstants}
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import controllers.website.EgraphsFunctionalTest

class PostCelebrityOrderApiEndpointTests extends EgraphsFunctionalTest {

  import FunctionalTest._
  private val db = AppConfig.instance[DBSession]
  private val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testRejectOrder() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    val ordersResponse = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    val orderId = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse)).head("id")

    val response = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/orders/" + orderId,
      APPLICATION_X_WWW_FORM_URLENCODED,
      "reviewStatus=" + OrderReviewStatus.RejectedByCelebrity.name + "&rejectionReason=It+made+me+cry"
    )
    assertIsOk(response)

    db.connected(TransactionSerializable) {
      val order = orderStore.get(orderId.toString.toLong)
      assertEquals(OrderReviewStatus.RejectedByCelebrity, order.reviewStatus)
      assertEquals("It made me cry", order.rejectionReason.get)
    }

  }
}
