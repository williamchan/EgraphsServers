package models.checkout

import Conversions._
import forms.BuyerDetails
import forms.FakeFormData._
import models.enums.CheckoutCodeType
import models.OrderStore
import services.AppConfig
import utils._
import scala.Some


class EgraphCheckoutAdapterTests extends EgraphsUnitTest
  with DateShouldMatchers
  with DBTransactionPerTest
  with HasTransientServicesTests[EgraphCheckoutAdapter]
{
  //
  // HasTransientServicesTests methods
  //
  override def newModel = {
    import LineItemTestData._
    EgraphCheckoutAdapter(
      0L,
      order = Some(randomEgraphOrderType()),
      coupon = Some(randomCouponType()),
      payment = Some(randomCashTransactionType),
      buyerDetails = Some(
        BuyerDetails(
          Some(TestData.generateFullname()),
          TestData.generateEmail()
        )
      )
    )
  }

  override def assertModelsEqual(left: EgraphCheckoutAdapter, right: EgraphCheckoutAdapter) {
    // check each component of the EgraphCheckoutAdapter, ignoring _services by copying from left to right before comparison
    for (leftOrder <- left.order; rightOrder <- right.order)
      leftOrder should be (rightOrder.copy(_services = leftOrder._services))

    // ugh, this...
    for (leftCoupon <- left.coupon; rightCoupon <- right.coupon){
      leftCoupon should be (
        rightCoupon.copy(
          coupon = rightCoupon.coupon.copy(_services = leftCoupon.coupon._services),
          _services = leftCoupon._services
        )
      )
    }

    for (leftPayment <- left.payment; rightPayment <- right.payment)
      leftPayment should be (rightPayment.copy(_services = leftPayment._services))

    left.buyerDetails should be (right.buyerDetails)
    left.recipientEmail should be (right.recipientEmail)
    left.shippingAddress should be (right.shippingAddress)
  }


  //
  // Test cases
  //
  "An EgraphCheckoutAdapter" should "transact successfully with order, buyer email, and payment" in {
    assertTransacts(newModel.withCoupon(None))
  }

  it should "transact with gift order, buyer, recipient, and payment" in {
    val (_, _, adapter) = BuyerRecipientAdapter(gift = true)
    assertTransacts(adapter)
  }

  it should "have reasonable buyer/recipient names and emails" in {
    val (buyer, _, adapter) = BuyerRecipientAdapter()

    adapter.buyer.account.email should be (buyer.email)
    adapter.buyer.customer.name should be (buyer.name)
    adapter.recipient should be (None)
  }

  it should "have reasonable buyer/recipient names and emails in print scenario" in {
    val (buyer, _, adapter) = BuyerRecipientAdapter(print = true)

    adapter.buyer.account.email should be (buyer.email)
    adapter.buyer.customer.name should be (buyer.name)
    adapter.recipient should be (None)
  }

  it should "have reasonable buyer/recipient names and emails in gift scenario" in {
    val (buyer, recipient, adapter) = BuyerRecipientAdapter(gift = true)

    adapter.buyer.account.email should be (buyer.email)
    buyer.email should startWith (adapter.buyer.customer.name)
    adapter.recipient should be ('defined)
    adapter.recipient.get.account.email should be (recipient.email)
    adapter.recipient.get.customer.name should be (recipient.name)
  }

  it should "have reasonable buyer/recipient names and emails in gift & print scenario" in {
    val (buyer, recipient, adapter) = BuyerRecipientAdapter(print = true, gift = true)

    adapter.buyer.account.email should be (buyer.email)
    adapter.buyer.customer.name should be (buyer.name)
    adapter.recipient should be ('defined)
    adapter.recipient.get.account.email should be (recipient.email)
    adapter.recipient.get.customer.name should be (recipient.name)
  }


  //
  // Helpers
  //
  private case class NameEmail(name: String, email: String)
  private def BuyerRecipientAdapter(print: Boolean = false, gift: Boolean = false) = {
    import TestData._

    val buyer = NameEmail(generateFullname(), generateEmail())
    val recipient = if (!gift) buyer else NameEmail(generateFullname(), generateEmail())
    val shippingAddress = randomShippingAddress.copy(name = buyer.name)

    val order = LineItemTestData.randomEgraphOrderType(withPrint = print, isGift = gift)
      .copy(recipientName = recipient.name)

    val adapter = newModel.withOrder(Some(order))
      .withBuyer( Some(BuyerDetails(None, buyer.email)) )
      .withRecipientEmail { if (gift) Some(recipient.email) else None }
      .withShippingAddress { if (print) Some(shippingAddress) else None }

    (buyer, recipient, adapter)
  }

  private def assertTransacts(adapter: EgraphCheckoutAdapter) {
    import CheckoutCodeType.EgraphOrder

    adapter should be ('validated)

    val transacted = adapter.transact().get

    transacted should be ('right)

    val orderStore = AppConfig.instance[OrderStore]
    val orderItemId = transacted.right.get.lineItems(EgraphOrder).head.id
    val restoredOrder = orderStore.findByLineItemId(orderItemId).headOption

    restoredOrder should be ('defined)

  }
}
