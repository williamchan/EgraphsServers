package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{
  willChanRequest, 
  runFreshScenarios, 
  routeName, 
  runWillChanScenariosThroughOrder
}
import utils.TestConstants
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.{postCelebrityOrder, getCelebrityOrders}

import models._
import enums.OrderReviewStatus
import services.db.TransactionSerializable
import services.AppConfig

class PostCelebrityOrderApiEndpointTests 
  extends EgraphsUnitTest
  with ProtectedCelebrityResourceTests
{
  protected override def routeUnderTest = postCelebrityOrder(1L)

  private def orderStore = AppConfig.instance[OrderStore]
  private def db = AppConfig.instance[services.db.DBSession]

  routeName(routeUnderTest) should "successfully reject orders with a given reason" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()
    
    val Some(ordersResult) = routeAndCall(willChanRequest.copy(GET, getCelebrityOrders(Some(true)).url))
    val orderId = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(ordersResult)).head("id").asInstanceOf[BigDecimal]

    val testRequest = willChanRequest
      .copy(method=POST, uri=postCelebrityOrder(orderId.toLong).url)
      .withFormUrlEncodedBody(
        "reviewStatus" -> OrderReviewStatus.RejectedByCelebrity.name,
        "rejectionReason" -> "It made me cry"
      )
    
    val Some(result) = routeAndCall(testRequest)
    
    status(result) should be (OK)

    db.connected(TransactionSerializable) {
      val order = orderStore.get(orderId.toString.toLong)
      order.reviewStatus should be (OrderReviewStatus.RejectedByCelebrity)
      order.rejectionReason should be (Some("It made me cry"))
    }

  }
}
