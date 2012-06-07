package controllers.website

import admin.AdminFunctionalTest
import org.junit.Test
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.{TestConstants, TestData}
import models.{Egraph, Order}

class GetEgraphEndpointTests extends AdminFunctionalTest {

  private val db = AppConfig.instance[DBSession]

  @Test
  def testPrivateEgraphsAreOnlyViewableByBuyerAndRecipientAndAdmin() {
    val (orderId: String, buyerAcct, recipientAcct, anotherAcct) = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val recipient = TestData.newSavedCustomer()
      val anotherCustomer = TestData.newSavedCustomer()
      val order = buyer.buy(TestData.newSavedProduct(), recipient = recipient)
        .copy(privacyStatus = Order.PrivacyStatus.Private.stateValue, reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue).save()
      order.newEgraph.copy(stateValue = Egraph.EgraphState.Published.value)
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.fakeAudio).save()
      (order.id.toString, buyer.account, recipient.account, anotherCustomer.account)
    }

    // anonymous users and random customers cannot view this egraph
    assertStatus(403, GET("/egraph/" + orderId))
    login(anotherAcct)
    assertStatus(403, GET("/egraph/" + orderId))

    // buyer, recipient, and admins are able to view this egraph
    login(buyerAcct)
    assertIsOk(GET("/egraph/" + orderId))
    login(recipientAcct)
    assertIsOk(GET("/egraph/" + orderId))
    createAndLoginAsAdmin()
    assertIsOk(GET("/egraph/" + orderId))
  }

  @Test
  def testPublicEgraphsAreViewableByAll() {
    val orderId: String = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val order = buyer.buy(TestData.newSavedProduct())
        .copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue).save()
      order.newEgraph.copy(stateValue = Egraph.EgraphState.Published.value)
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.fakeAudio).save()
      order.id.toString
    }

    assertIsOk(GET("/egraph/" + orderId))
  }
}
