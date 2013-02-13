package controllers.api

import play.api.test.FakeRequest
import utils.FunctionalTestUtils.{
  runCustomerBuysProductsScenerio,
  routeName,
  requestWithCredentials
}
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import utils.TestConstants
import utils.FunctionalTestUtils.EgraphsFakeRequest

import models._
import enums.OrderReviewStatus
import services.db.TransactionSerializable
import services.AppConfig

class PostCelebrityOrderApiEndpointTests 
  extends EgraphsUnitTest
  with ProtectedCelebrityResourceTests
{
  protected override def routeUnderTest = controllers.routes.ApiControllers.postCelebrityOrder(1L)

  private def orderStore = AppConfig.instance[OrderStore]
  private def db = AppConfig.instance[services.db.DBSession]

  routeName(routeUnderTest) should "successfully reject orders with a given reason" in new EgraphsTestApplication {
    val (celebrityAccount, order) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders.head)
    }

    val url = controllers.routes.ApiControllers.postCelebrityOrder(order.id).url
    val Some(result) = route(
      FakeRequest(POST, url).withCredentials(celebrityAccount)
      .withFormUrlEncodedBody(
        "reviewStatus" -> OrderReviewStatus.RejectedByCelebrity.name,
        "rejectionReason" -> "It made me cry"
      )
    )

    status(result) should be(OK)

    db.connected(TransactionSerializable) {
      val actualOrder = orderStore.get(order.id)
      actualOrder.reviewStatus should be(OrderReviewStatus.RejectedByCelebrity)
      actualOrder.rejectionReason should be(Some("It made me cry"))
    }
  }
}
