package controllers.website

import play.test.FunctionalTest
import org.junit.Test
import services.AppConfig
import scala.collection.JavaConversions._
import FunctionalTest._
import org.junit.Assert._
import models._
import utils.TestData
import org.squeryl.PrimitiveTypeMode._
import org.joda.money.CurrencyUnit
import models.CashTransaction.EgraphPurchase
import services.db.{Schema, TransactionSerializable, DBSession}

class PostBuyProductEndpointTests extends EgraphsFunctionalTest {
  private val db = AppConfig.instance[DBSession]
  private val schema = AppConfig.instance[Schema]
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
      val order = allCelebOrders.head
      assertNotNull(order.stripeChargeId.get)

      val cashTransaction = from(schema.cashTransactions)(txn =>
        where(txn.orderId === Some(order.id))
          select (txn)
      ).head
      assertNotNull(cashTransaction.accountId)
      assertEquals(order.id, cashTransaction.orderId.get)
      assertEquals(product.priceInCurrency, cashTransaction.amountInCurrency)
      assertEquals(CurrencyUnit.USD.getCode, cashTransaction.currencyCode)
      assertEquals(EgraphPurchase.value, cashTransaction.typeString)
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
      assertEquals(0, from(schema.cashTransactions)(txn => select (txn)).size) // no CashTransactions should have been created
    }
  }
}
