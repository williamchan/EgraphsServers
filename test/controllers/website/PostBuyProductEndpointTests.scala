package controllers.website

import play.test.FunctionalTest
import org.junit.Test
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import services.AppConfig
import scala.collection.JavaConversions._
import FunctionalTest._
import services.db.{TransactionSerializable, DBSession}
import org.junit.Assert._
import models._
import utils.TestData

class PostBuyProductEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {
  private val db = AppConfig.instance[DBSession]
  private val orderStore = AppConfig.instance[OrderStore]

  @Test
  def testBuyEgraphSucceeds() {
    val (celebrity: Celebrity, product: Product) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      val product = TestData.newSavedProduct(Some(celebrity))
      (celebrity, product)
    }

    val response = POST("/" + celebrity.urlSlug.get + "/" + product.urlSlug + "/buy",
      Map(
        "recipientName" -> "Erem Recipient",
        "recipientEmail" -> "erem@egraphs.com",
        "buyerName" -> "Erem Buyer",
        "buyerEmail" -> "erem@egraphs.com",
        "stripeTokenId" -> "token"
      )
    )

    assertStatus(302, response)
    assertHeaderEquals("Location", "/orders/1/confirm", response)
    db.connected(TransactionSerializable) {
      val allCelebOrders = orderStore.findByCelebrity(celebrity.id)
      assertEquals(1, allCelebOrders.toList.length)
      assertNotNull(allCelebOrders.head.stripeChargeId.get)
    }
  }

  @Test
  def testBuyEgraphFailsToSaveOrderWhenThereIsInsufficientInventory() {
    val (celebrity: Celebrity, product: Product) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      val product = TestData.newSavedProduct(Some(celebrity))
      product.inventoryBatches.head.copy(numInventory = 0).save()
      (celebrity, product)
    }

    val response = POST("/" + celebrity.urlSlug.get + "/" + product.urlSlug + "/buy",
      Map(
        "recipientName" -> "Erem Recipient",
        "recipientEmail" -> "erem@egraphs.com",
        "buyerName" -> "Erem Buyer",
        "buyerEmail" -> "erem@egraphs.com",
        "stripeTokenId" -> "token"
      )
    )

    assertStatus(302, response)
    db.connected(TransactionSerializable) {
      val allCelebOrders = orderStore.findByCelebrity(celebrity.id)
      assertEquals(0, allCelebOrders.toList.length)
    }
  }
}
