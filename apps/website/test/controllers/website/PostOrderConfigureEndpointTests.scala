package controllers.website

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import scala.collection.JavaConversions._
import FunctionalTest._
import services.AppConfig
import models.OrderStore
import services.db.TransactionSerializable
import utils.TestData
import models.enums.PrivacyStatus


class PostOrderConfigureEndpointTests extends EgraphsFunctionalTest {
  private val orderStore = AppConfig.instance[OrderStore]

  @Test
  def ChangesPrivacyStatusOfOrderTest() = {
  val account =  db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      val order = customer.buy(TestData.newSavedProduct()).save()
      customer.account
    }

    login(account)

    val response = POST("/orders/1/configure", Map("privacyStatus" -> PrivacyStatus.Private.name))
    assertStatus(200, response)
    db.connected(TransactionSerializable) {
      assertEquals(orderStore.get(1).privacyStatus, PrivacyStatus.Private)
    }
    val response1 = POST("/orders/1/configure", Map("privacyStatus" -> PrivacyStatus.Public.name))

    assertStatus(200, response1)
    db.connected(TransactionSerializable) {
      assertEquals(orderStore.get(1).privacyStatus, PrivacyStatus.Public)
    }

  }
}
