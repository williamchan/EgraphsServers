package controllers.api.getorders

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}
import org.squeryl.PrimitiveTypeMode._
import scenario.Scenarios
import models._
import utils.TestConstants
import db.DBSession
import services.AppConfig


class CelebrityGetOrdersFiltersVerifiedTests extends FunctionalTest with CleanDatabaseAfterEachTest {

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
      val allCelebOrders = orderStore.FindByCelebrity(celebrityId)
      Egraph(orderId = allCelebOrders.toSeq.head.id).withState(Verified).saveWithoutAssets()
    }

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertEquals(json.length, 1)
  }

}