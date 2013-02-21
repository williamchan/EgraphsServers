package controllers.api

import play.api.mvc.Result
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scenario.Scenarios
import utils.FunctionalTestUtils._
import utils.TestConstants
import utils.EgraphsUnitTest
import services.Utils
import services.AppConfig
import services.config.ConfigFileProxy
import services.db.{ DBSession, TransactionSerializable }
import models._
import enums.EgraphState
import utils.TestData
import services.db.TransactionSerializable


class GetCelebrityOrdersApiEndpointTests
  extends EgraphsUnitTest
  with ProtectedCelebrityResourceTests
{
  protected def routeUnderTest = controllers.routes.ApiControllers.getCelebrityOrders()
  private def orderStore = AppConfig.instance[OrderStore]
  private def db = AppConfig.instance[DBSession]

  routeName(routeUnderTest) should "get the list of celebrity orders" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders)
    }

    // Execute the request
    val result = routeGetCelebrityOrders(celebrityAccount)

    status(result) should be(OK)

    val ordersInResponse = getOrdersFromResult(result)

    // Just check the ids -- the rest is covered by unit tests
    for (order <- orders) {
      ordersInResponse.exists(inResponse => inResponse.id  == order.id) should be(true)
    }
  }

  it should "require the signerActionable query parameter" in new EgraphsTestApplication {
    val (celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      celebrity.account
    }

    val result = routeGetCelebrityOrders(celebrityAccount, signerActionable = None)

    status(result) should be(BAD_REQUEST)
  }

  it should "filter out orders with egraphs that have been fulfilled but await biometric verification" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be awaiting verification
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.AwaitingVerification).save()

      (celebrity.account, orders)
    }

    val result = routeGetCelebrityOrders(celebrityAccount)
    status(result) should be(OK)

    val ordersInResponse = getOrdersFromResult(result)
    ordersInResponse.size should be(3) // 4 - 1 (the one we made awaiting verification)
  }

  it should "filter out orders with Egraphs that have already been published" in {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be published
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.Published).save()

      (celebrity.account, orders)
    }

    val result = routeGetCelebrityOrders(celebrityAccount)
    status(result) should be(OK)

    val ordersInResponse = getOrdersFromResult(result)
    ordersInResponse.size should be(3) // 4 - 1 (the one we made published)
  }

  it should "include orders with with egraphs that were rejected" in {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be rejected
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.RejectedByAdmin).save()

      (celebrity.account, orders)
    }

    val result = routeGetCelebrityOrders(celebrityAccount)
    status(result) should be(OK)

    val ordersInResponse = getOrdersFromResult(result)
    ordersInResponse.size should be(4)
  }

  private def getOrdersFromResult(result: Result): Seq[Order] = {
    val jsonOrders = Json.parse(contentAsString(result)).asInstanceOf[JsArray].value
    jsonOrders.map(_.as[Order])
  }

  /**
   * Assemble the request and get the result.
   */
  private def routeGetCelebrityOrders(celebrityAccount: Account, signerActionable: Option[Boolean] = Some(true)): Result = {
    val url = controllers.routes.ApiControllers.getCelebrityOrders(signerActionable = signerActionable).url
    val req = FakeRequest(GET, url).withCredentials(celebrityAccount)
    val Some(result) = route(req)
    result
  }
}