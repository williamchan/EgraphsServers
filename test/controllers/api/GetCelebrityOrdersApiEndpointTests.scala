package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}
import scenario.Scenarios
import models._
import utils.TestConstants
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}

class GetCelebrityOrdersApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  private val db = AppConfig.instance[DBSession]
  private val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testGetCelebrityOrders() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

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
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(EgraphState.AwaitingVerification).saveWithoutAssets()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }

  @Test
  def testGetOrdersFiltersOutOrdersWithEgraphWithVerifiedState() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(EgraphState.Verified).saveWithoutAssets()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }

  @Test
  def testGetOrdersIncludesOrdersWithRejectedEgraphs() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    db.connected(TransactionSerializable) {
      import EgraphState._

      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(RejectedVoice).saveWithoutAssets()
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(RejectedSignature).saveWithoutAssets()
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(RejectedBoth).saveWithoutAssets()
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(RejectedPersonalAudit).saveWithoutAssets()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 2)
  }
}
