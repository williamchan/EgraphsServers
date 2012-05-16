package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}
import scenario.Scenarios
import models._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.{FunctionalTestUtils, TestConstants}
import models.Egraph.EgraphState

class GetCelebrityOrdersApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  private val db = AppConfig.instance[DBSession]
  private val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testGetCelebrityOrders() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    // Assemble the request
    val req = willChanRequest

    // Execute the request
    val response = GET(req, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))

    val firstOrderJson = json(0)
    val secondOrderJson = json(1)

    // Just check the ids -- the rest is covered by unit tests
    assertEquals(BigDecimal(1L), firstOrderJson("id"))
    assertEquals(BigDecimal(2L), secondOrderJson("id"))
  }

  @Test
  def testGetCelebrityOrdersRequiresSignerActionableParameter() {
    runScenarios(
      "Will-Chan-is-a-celebrity"
    )

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders")
    assertStatus(500, response)
  }

  @Test
  def testGetOrdersFiltersOutOrdersWithEgraphWithAwaitingVerificationState() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(EgraphState.AwaitingVerification).save()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }

  @Test
  def testGetOrdersFiltersOutOrdersWithEgraphWithPublishedState() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(EgraphState.Published).save()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }

  @Test
  def testGetOrdersIncludesOrdersWithRejectedEgraphs() {
    FunctionalTestUtils.runWillChanScenariosThroughOrder()

    db.connected(TransactionSerializable) {
      import EgraphState._

      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(RejectedByAdmin).save()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 2)
  }
}
