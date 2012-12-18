package controllers.api

import sjson.json.Serializer
import scenario.Scenarios
import utils.FunctionalTestUtils.{
  routeName,
  requestWithCredentials,
  runCustomerBuysProductsScenerio
}
import utils.TestConstants
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import services.Utils
import services.AppConfig
import services.config.ConfigFileProxy
import services.db.{ DBSession, TransactionSerializable }
import models._
import enums.EgraphState
import utils.TestData
import services.db.TransactionSerializable
import play.api.mvc.Result

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
    val result = routeAndCallGetCelebrityOrders(celebrityAccount)

    status(result) should be(OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    val orderIdsInResponse = json.map(aJson => aJson("id"))

    // Just check the ids -- the rest is covered by unit tests
    for (order <- orders) {
      orderIdsInResponse.contains(BigDecimal(order.id)) should be(true)
    }
  }

  it should "require the signerActionable query parameter" in new EgraphsTestApplication {
    val (celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      celebrity.account
    }

    val result = routeAndCallGetCelebrityOrders(celebrityAccount, signerActionable = None)

    status(result) should be(BAD_REQUEST)
  }

  it should "filter out orders with egraphs that have been fulfilled but await biometric verification" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be awaiting verification
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.AwaitingVerification).save()

      (celebrity.account, orders)
    }

    val result = routeAndCallGetCelebrityOrders(celebrityAccount)
    status(result) should be(OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json.length should be(3) // 4 - 1 (the one we made awaiting verification)
  }

  it should "filter out orders with Egraphs that have already been published" in {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be published
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.Published).save()

      (celebrity.account, orders)
    }

    val result = routeAndCallGetCelebrityOrders(celebrityAccount)
    status(result) should be(OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json.length should be(3) // 4 - 1 (the one we made published)
  }

  it should "include orders with with egraphs that were rejected" in {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be rejected
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.RejectedByAdmin).save()

      (celebrity.account, orders)
    }

    val result = routeAndCallGetCelebrityOrders(celebrityAccount)
    status(result) should be(OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json.length should be(4)
  }

  /**
   * Assemble the request and get the result.
   */
  private def routeAndCallGetCelebrityOrders(celebrityAccount: Account, signerActionable: Option[Boolean] = Some(true)): Result = {
    val url = controllers.routes.ApiControllers.getCelebrityOrders(signerActionable = signerActionable).url
    val req = requestWithCredentials(celebrityAccount).copy(method = GET, uri = url)
    val Some(result) = routeAndCall(req)
    result
  }
}