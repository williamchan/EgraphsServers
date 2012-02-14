package controllers.nonproduction

import play.test.FunctionalTest
import org.junit.Test
import utils.{ClearsDatabaseAndValidationAfter, FunctionalTestUtils}
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest


class PostBuyDemoProductEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest  {
  import scala.collection.JavaConversions._
  import FunctionalTest._

  @Test
  def testBuyEgraphFail() {
    FunctionalTestUtils.runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products"
    )

    val response = POST("/Wizzle/2010-Starcraft-2-Championships/buy-demo",
      Map("herp" -> "derp")
    )

    assertStatus(302, response)
    assertHeaderEquals("Location", "/Wizzle/2010-Starcraft-2-Championships", response)
  }

  @Test
  def testBuyEgraphSucceed() {
    FunctionalTestUtils.runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products"
    )

    val response = POST("/Wizzle/2010-Starcraft-2-Championships/buy-demo",
      Map(
        "recipientName" -> "Erem Recipient",
        "recipientEmail" -> "erem@egraphs.com",
        "buyerName" -> "Erem Buyer",
        "buyerEmail" -> "erem@egraphs.com"
      )
    )

    assertStatus(302, response)
    assertHeaderEquals("Location", "/orders/1/confirm", response)
  }

}