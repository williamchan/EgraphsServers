package controllers.api

import sjson.json.Serializer
import scenario.Scenarios
import utils.FunctionalTestUtils.{
  runFreshScenarios,
  routeName,
  requestWithCredentials,
  runCustomerBuysProductsScenerio
}
import utils.TestConstants
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.getCelebrityOrders
import services.Utils
import services.AppConfig
import services.config.ConfigFileProxy
import services.db.{ DBSession, TransactionSerializable }
import models._
import enums.EgraphState
import utils.TestData
import services.db.TransactionSerializable
import scenario.RepeatableScenarios

class GetCelebrityOrdersApiEndpointTests
  extends EgraphsUnitTest //TODO: (myyk) Re-enable these before checking
  //  with ProtectedCelebrityResourceTests
  {
  protected def routeUnderTest = getCelebrityOrders()
  private def orderStore = AppConfig.instance[OrderStore]
  private def db = AppConfig.instance[DBSession]

  routeName(routeUnderTest) should "get the list of celebrity orders" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()
      (celebrity.account, orders)
    }

    // Assemble the request
    val url = getCelebrityOrders(signerActionable = Some(true)).url
    val req = requestWithCredentials(celebrityAccount).copy(method = GET, uri = url)

    // Execute the request
    val Some(result) = routeAndCall(req)

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
      RepeatableScenarios.createCelebrity(isFeatured = true).account
    }

    val url = getCelebrityOrders(signerActionable = None).url
    val req = requestWithCredentials(celebrityAccount).copy(method = GET, uri = url)
    val Some(result) = routeAndCall(req)

    status(result) should be(BAD_REQUEST)
  }

  it should "filter out orders with egraphs that have been fulfilled but await biometric verification" in new EgraphsTestApplication {
    val (celebrityAccount, orders) = db.connected(TransactionSerializable) {
      val (_, celebrity, _, orders) = runCustomerBuysProductsScenerio()

      // make the first order be awaiting verification
      Egraph(orderId = orders.head.id).withEgraphState(EgraphState.AwaitingVerification).save()

      println(orders.map(_.id))
      (celebrity.account, orders)
    }

    val url = getCelebrityOrders(signerActionable = Some(true)).url
    val req = requestWithCredentials(celebrityAccount).copy(method = GET, uri = url)
    val Some(result) = routeAndCall(req)
    status(result) should be(OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    println(json)
    json.length should be(3) // 4 - 1 (the one we made awaiting verification)
  }

  //  it should "filter out orders with Egraphs that have already been published" in {
  //    runWillChanScenariosThroughOrder()
  //
  //    db.connected(TransactionSerializable) {
  //      val celebrityId = Scenarios.getWillCelebrityAccount.id
  //      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
  //      Egraph(orderId = allCelebOrders.toSeq.head.id).withEgraphState(EgraphState.Published).save()
  //    }
  //
  //    val url = getCelebrityOrders(signerActionable=Some(true)).url
  //    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=url))
  //    status(result) should be (OK)
  //
  //    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
  //    json.length should be (1)
  //  }
  //
  //  it should "include orders with with egraphs that were rejected" in {
  //    runWillChanScenariosThroughOrder()
  //
  //    db.connected(TransactionSerializable) {
  //      val celebrityId = Scenarios.getWillCelebrityAccount.id
  //      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
  //      Egraph(orderId = allCelebOrders.toSeq.head.id).withEgraphState(EgraphState.RejectedByAdmin).save()
  //    }
  //
  //    val url = getCelebrityOrders(signerActionable=Some(true)).url
  //    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=url))
  //    status(result) should be (OK)
  //
  //    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
  //    json.length should be (2)
  //  }
}
