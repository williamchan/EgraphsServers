package controllers.nonproduction

import play.test.FunctionalTest
import org.junit.Test
import utils.FunctionalTestUtils
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import services.AppConfig
import scala.collection.JavaConversions._
import FunctionalTest._
import services.db.{TransactionSerializable, DBSession}
import scenario.Scenarios
import models.OrderStore
import org.junit.Assert._

class PostBuyDemoProductEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest  {
  private val db = AppConfig.instance[DBSession]
  private val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testBuyEgraphFail() {
    FunctionalTestUtils.runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products"
    )

    POST("/Wizzle/2010-Starcraft-2-Championships/buy-demo",
      Map("herp" -> "derp")
    )

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      assertEquals(0, allCelebOrders.toList.length)
    }
  }

  @Test
  def testBuyEgraphSucceed() {
    FunctionalTestUtils.runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products"
    )

    POST("/Wizzle/2010-Starcraft-2-Championships/buy-demo",
      Map(
        "recipientName" -> "Erem Recipient",
        "recipientEmail" -> "erem@egraphs.com",
        "buyerName" -> "Erem Buyer",
        "buyerEmail" -> "erem@egraphs.com"
      )
    )

    db.connected(TransactionSerializable) {
      val celebrityId = Scenarios.getWillCelebrityAccount.id
      val allCelebOrders = orderStore.findByCelebrity(celebrityId)
      assertEquals(1, allCelebOrders.toList.length)
    }
  }

}