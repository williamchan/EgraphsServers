package controllers.website

import admin.AdminFunctionalTest
import org.junit.Test
import play.test.FunctionalTest
import FunctionalTest._
import services.db.TransactionSerializable
import utils.{TestConstants, TestData}
import models.enums.{EgraphState, OrderReviewStatus, PrivacyStatus}
import play.libs.Codec

class GetEgraphEndpointTests extends AdminFunctionalTest {

  @Test
  def testPrivateEgraphsAreOnlyViewableByBuyerAndRecipientAndAdmin() {
    val (orderId: String, buyerAcct, recipientAcct, anotherAcct) = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val recipient = TestData.newSavedCustomer()
      val anotherCustomer = TestData.newSavedCustomer()
      val order = buyer.buy(TestData.newSavedProduct(), recipient = recipient)
        .withPrivacyStatus(PrivacyStatus.Private)
        .withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      order.newEgraph.withEgraphState(EgraphState.Published)
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), Codec.decodeBASE64(TestConstants.voiceStr())).save()
      (order.id.toString, buyer.account, recipient.account, anotherCustomer.account)
    }

    // anonymous users and random customers are redirected away
    assertStatus(403, GET("/" + orderId))
    login(anotherAcct)
    assertStatus(403, GET("/" + orderId))

    // buyer, recipient, and admins are able to view this egraph
    login(buyerAcct)
    assertIsOk(GET("/" + orderId))
    login(recipientAcct)
    assertIsOk(GET("/" + orderId))
    createAndLoginAsAdmin()
    assertIsOk(GET("/" + orderId))
  }

  @Test
  def testPublicEgraphsAreViewableByAll() {
    val orderId: String = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val order = buyer.buy(TestData.newSavedProduct())
        .withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      order.newEgraph.withEgraphState(EgraphState.Published)
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), Codec.decodeBASE64(TestConstants.voiceStr())).save()
      order.id.toString
    }

    assertIsOk(GET("/" + orderId))
  }
}
