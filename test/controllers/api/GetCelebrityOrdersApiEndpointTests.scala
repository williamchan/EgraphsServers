package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}
import org.squeryl.PrimitiveTypeMode._
import scenario.Scenarios
import models._
import utils.TestConstants
import services.db.DBSession
import services.AppConfig

class GetCelebrityOrdersApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

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
}

/**
 * This FunctionalTest is the only test in its class because multiple FunctionalTests don't play nicely with direct
 * database transactions. See issue #40.
 */
class GetCelebrityOrdersFiltersAwaitingVerificationApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testGetOrdersFiltersOutOrdersWithEgraphWithAwaitingVerificationState() {
    DBSession.init()
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    transaction {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(EgraphState.AwaitingVerification).saveWithoutAssets()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }
}

/**
 * This FunctionalTest is the only test in its class because multiple FunctionalTests don't play nicely with direct
 * database transactions. See issue #40.
 */
class GetCelebrityOrdersFiltersVerifiedApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testGetOrdersFiltersOutOrdersWithEgraphWithVerifiedState() {
    DBSession.init()
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    transaction {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(EgraphState.Verified).saveWithoutAssets()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }
}

/**
 * This FunctionalTest is the only test in its class because multiple FunctionalTests don't play nicely with direct
 * database transactions. See issue #40.
 */
class GetCelebrityOrdersIncludesRejectedApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testGetOrdersIncludesOrdersWithRejectedEgraphs() {
    DBSession.init()
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    transaction {
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
