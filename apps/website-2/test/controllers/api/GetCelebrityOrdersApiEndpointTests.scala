package controllers.api

import sjson.json.Serializer
import scenario.Scenarios
import utils.FunctionalTestUtils.{
  willChanRequest, 
  runFreshScenarios, 
  routeName, 
  runWillChanScenariosThroughOrder
}
import utils.TestConstants
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.getCelebrityOrders
import services.Utils
import services.AppConfig
import services.config.ConfigFileProxy
import services.db.{DBSession, TransactionSerializable}

import models._
import enums.EgraphState
// import utils.{FunctionalTestUtils, TestConstants}

class GetCelebrityOrdersApiEndpointTests 
  extends EgraphsUnitTest 
  with ProtectedCelebrityResourceTests
{
  protected def routeUnderTest = getCelebrityOrders()
  private def orderStore = AppConfig.instance[OrderStore]
  private def db = AppConfig.instance[DBSession]

  routeName(routeUnderTest) should "get the list of celebrity orders" in new EgraphsTestApplication {
    runWillChanScenariosThroughOrder()

    // Assemble the request
    val url = getCelebrityOrders(signerActionable=Some(true)).url
    val req = willChanRequest.copy(method=GET, uri=url)

    // Execute the request
    val Some(result) = routeAndCall(req)
    
    status(result) should be (OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    val firstOrderJson = json(0)
    val secondOrderJson = json(1)

    // Just check the ids -- the rest is covered by unit tests
    firstOrderJson("id") should be (BigDecimal(1L))
    secondOrderJson("id") should be (BigDecimal(2L))
  }

  it should  "require the signerActionable query parameter" in new EgraphsTestApplication {
    runFreshScenarios(
      "Will-Chan-is-a-celebrity"
    )

    val url = getCelebrityOrders(signerActionable=None).url
    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=url))
    
    status(result) should be (BAD_REQUEST)
  }
  

  it should "filter out orders with egraphs that have been fulfilled but await biometric verification" in new EgraphsTestApplication {    
    runWillChanScenariosThroughOrder()

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withEgraphState(EgraphState.AwaitingVerification).save()
    }

    val url = getCelebrityOrders(signerActionable=Some(true)).url
    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=url))
    status(result) should be (OK)
    
    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json.length should be (1)
  }

  it should "filter out orders with Egraphs that have already been published" in {
    runWillChanScenariosThroughOrder()

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withEgraphState(EgraphState.Published).save()
    }

    val url = getCelebrityOrders(signerActionable=Some(true)).url
    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=url))
    status(result) should be (OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json.length should be (1)
  }

  it should "include orders with with egraphs that were rejected" in {
    runWillChanScenariosThroughOrder()

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withEgraphState(EgraphState.RejectedByAdmin).save()
    }

    val url = getCelebrityOrders(signerActionable=Some(true)).url
    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=url))
    status(result) should be (OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json.length should be (2)
  }
}
