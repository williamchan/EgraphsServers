package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest}
import models._
import utils.{FunctionalTestUtils, TestConstants}
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig

class PostCelebrityOrderApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

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
      "reviewStatus=" + Order.ReviewStatus.RejectedByCelebrity.stateValue + "&rejectionReason=It+made+me+cry"
    )
    assertIsOk(response)

    db.connected(TransactionSerializable) {
      val order = orderStore.findById(orderId.toString.toLong).get
      assertEquals(Order.ReviewStatus.RejectedByCelebrity.stateValue, order.reviewStatus)
      assertEquals("It made me cry", order.rejectionReason.get)
    }

  }
}
