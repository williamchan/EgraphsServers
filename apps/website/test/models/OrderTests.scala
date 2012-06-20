package models

import enums.{EgraphState, OrderReviewStatus, PaymentStatus}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig
import org.squeryl.PrimitiveTypeMode._
import services.db.Schema
import org.joda.money.CurrencyUnit
import models.CashTransaction.{PurchaseRefund, EgraphPurchase}
import services.payment.{Charge, NiceCharge}
import javax.mail.internet.InternetAddress

class OrderTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Order]
  with CreatedUpdatedEntityTests[Order]
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
{
  private val schema = AppConfig.instance[Schema]
  private val orderStore = AppConfig.instance[OrderStore]
  private val orderQueryFilters = AppConfig.instance[OrderQueryFilters]

  //
  // SavingEntityTests[Order] methods
  //
  override def newEntity = {
    val (customer, product) = newCustomerAndProduct

    customer.buy(product)
  }

  override def saveEntity(toSave: Order) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    orderStore.findById(id)
  }

  override def transformEntity(toTransform: Order) = {
    val (customer, product) = newCustomerAndProduct
    val order = customer.buy(product)
    toTransform.copy(
      productId = order.productId,
      buyerId = order.buyerId,
      recipientId = order.recipientId,
      recipientName = "Derpy Jones",
      stripeCardTokenId = Some("12345"),
      stripeChargeId = Some("12345"),
      messageToCelebrity = Some("Wizzle you're the best!"),
      requestedMessage = Some("Happy birthday, Erem!")
    ).withPaymentStatus(PaymentStatus.Charged)
  }

  //
  // Test cases
  //

  "Order" should "require certain fields" in {
    val exception = intercept[IllegalArgumentException] {Order().save()}
    exception.getLocalizedMessage.contains("Order: recipientName must be specified") should be(true)
  }

  "An order" should "create Egraphs that are properly configured" in {
    val egraph = Order(id=100L).newEgraph

    egraph.orderId should be (100L)
    egraph.egraphState should be (EgraphState.AwaitingVerification)
  }

  it should "start out not charged" in {
    Order().paymentStatus should be (PaymentStatus.NotCharged)
  }

  it should "update payment state correctly" in {
    Order().withPaymentStatus(PaymentStatus.Charged).paymentStatus should be (PaymentStatus.Charged)
  }

  "renderedForApi" should "serialize the correct Map for the API" in {
    val order = newEntity.copy(requestedMessage = Some("requestedMessage"), messageToCelebrity = Some("messageToCelebrity")).save()
    val buyer = order.buyer

    val rendered = order.renderedForApi
    rendered("id") should be(order.id)
    rendered("buyerId") should be(buyer.id)
    rendered("buyerName") should be(buyer.name)
    rendered("recipientId") should be(buyer.id)
    rendered("recipientName") should be(buyer.name)
    rendered("amountPaidInCents") should be(order.amountPaid.getAmountMinor)
    rendered("reviewStatus") should be(order.reviewStatus.name)
    rendered("requestedMessage") should be(order.requestedMessage.get)
    rendered("messageToCelebrity") should be(order.messageToCelebrity.get)
    rendered.contains("audioPrompt") should be(true)
    rendered("orderType") should be(order.orderType.name)
    rendered.contains("created") should be(true)
    rendered.contains("updated") should be(true)
    rendered.contains("product") should be(true)
  }

  "generateAudioPrompt" should "generate audio prompt from audioPromptTemplates" in {
    val signer_name = "Signer"
    val recipient_name = "Recipient"
    val order = TestData.newSavedOrder().copy(recipientName = recipient_name).save()
    order.product.celebrity.copy(publicName = Some(signer_name)).save()

    order.generateAudioPrompt(Some(0)) should be("Yo Recipient, it’s Signer. It was awesome getting your message. Hope you enjoy this egraph.")
    order.generateAudioPrompt(Some(1)) should be("Recipient, it’s Signer here. Thanks for being a great fan. Hopefully we can win some games for you down the stretch.")
    order.generateAudioPrompt(Some(2)) should be("Hey Recipient, it’s Signer. Hope you’re having a great day. Thanks for the support!")
    order.generateAudioPrompt(Some(3)) should be("This is Signer. Recipient, thanks so much for reaching out to me. I really appreciated your message. Enjoy this egraph!")
    order.generateAudioPrompt(Some(4)) should be("Hey, Recipient, it’s Signer. I’ll look for you to post this egraph on twitter!")
    order.generateAudioPrompt(Some(5)) should be("Recipient, it’s Signer. Keep swinging for the fences.")
    order.generateAudioPrompt(Some(6)) should be("What’s up, Recipient? It’s Signer here. Thanks for connecting with me. Hope you dig this egraph and share it with your friends.")
    order.generateAudioPrompt(Some(7)) should be("Recipient, it’s Signer here. I hope you enjoy this egraph. It’s a great way for me to connect with you during the season. Have a great one!")
    order.generateAudioPrompt(Some(8)) should be("Hey, it’s Signer creating this egraph for Recipient. Thanks for being an awesome fan.")
    order.generateAudioPrompt(Some(9)) should be("Hey, Recipient, it’s Signer here. Thanks for reaching out to me through Egraphs. Have a great day.")
  }

  "approveByAdmin" should "change reviewStatus to ApprovedByAdmin" in {
    val order = newEntity.save()
    order.reviewStatus should be (OrderReviewStatus.PendingAdminReview)
    intercept[IllegalArgumentException] {order.approveByAdmin(null)}
    val admin = Administrator().save()
    order.approveByAdmin(admin).save().reviewStatus should be (OrderReviewStatus.ApprovedByAdmin)
  }

  "rejectByAdmin" should "change reviewStatus to RejectedByAdmin" in {
    val order = newEntity.save()
    order.reviewStatus should be (OrderReviewStatus.PendingAdminReview)
    order.rejectionReason should be (None)

    intercept[IllegalArgumentException] {order.rejectByAdmin(null)}

    val rejectedOrder = order.rejectByAdmin(Administrator().save(), Some("It made me cry")).save()
    rejectedOrder.reviewStatus should be (OrderReviewStatus.RejectedByAdmin)
    rejectedOrder.rejectionReason.get should be ("It made me cry")
  }

  "rejectByCelebrity" should "change reviewStatus to RejectedByCelebrity" in {
    val order = newEntity.withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    order.reviewStatus should be (OrderReviewStatus.ApprovedByAdmin)
    order.rejectionReason should be (None)

    intercept[IllegalArgumentException] {newEntity.save().rejectByCelebrity(null)}
    intercept[IllegalArgumentException] {order.rejectByCelebrity(null)}
    intercept[IllegalArgumentException] {order.rejectByCelebrity(Celebrity())}

    val rejectedOrder = order.rejectByCelebrity(order.product.celebrity, Some("It made me cry")).save()
    rejectedOrder.reviewStatus should be (OrderReviewStatus.RejectedByCelebrity)
    rejectedOrder.rejectionReason.get should be ("It made me cry")
  }

  "withChargeInfo" should "set the PaymentStatus, store stripe info, and create an associated CashTransaction" in {
    val (will, _, _, product) = newOrderStack
    val order = will.buy(product).save().withChargeInfo(stripeCardTokenId = "mytoken", stripeCharge = NiceCharge).save()

    // verify PaymentStatus
    order.paymentStatus should be(PaymentStatus.Charged)

    // verify Stripe Info
    order.stripeCardTokenId.get should be("mytoken")
    order.stripeChargeId.get.contains("test charge against services.payment.YesMaamPayment") should be(true)

    // verify CashTransaction
    val cashTransaction = from(schema.cashTransactions)(txn =>
      where(txn.orderId === Some(order.id))
        select (txn)
    ).head
    cashTransaction.accountId should be(will.account.id)
    cashTransaction.orderId should be(Some(order.id))
    cashTransaction.amountInCurrency should be(product.priceInCurrency)
    cashTransaction.currencyCode should be(CurrencyUnit.USD.getCode)
    cashTransaction.typeString should be(EgraphPurchase.value)
  }

  "refund" should "refund the Stripe charge, change the PaymentStatus to Refunded, and create a refund CashTransaction" in {
    val (will, _, _, product) = newOrderStack
    val order = will.buy(product).save().withChargeInfo(stripeCardTokenId = "mytoken", stripeCharge = NiceCharge).save()

    var (refundedOrder: Order, refundCharge: Charge) = order.refund()
    refundedOrder = refundedOrder.save()
    refundedOrder.paymentStatus should be(PaymentStatus.Refunded)
    refundCharge.refunded should be(true)

    val cashTransactions = from(schema.cashTransactions)(txn => where(txn.orderId === Some(order.id)) select (txn))
    cashTransactions.size should be(2)
    val purchaseTxn = cashTransactions.find(b => b.transactionType == EgraphPurchase).head
    purchaseTxn.accountId should be(will.id)
    purchaseTxn.orderId should be(Some(order.id))
    purchaseTxn.amountInCurrency should be(BigDecimal(product.price.getAmount))
    purchaseTxn.currencyCode should be(CurrencyUnit.USD.getCode)
    val refundTxn = cashTransactions.find(b => b.transactionType == PurchaseRefund).head
    refundTxn.accountId should be(will.id)
    refundTxn.orderId should be(Some(order.id))
    refundTxn.amountInCurrency should be(BigDecimal(product.price.negated().getAmount))
    refundTxn.currencyCode should be(CurrencyUnit.USD.getCode)
  }

  "prepareEgraphsSignedEmail" should "not use celebrity's email" in {
    val celebrity = TestData.newSavedCelebrity().copy(publicName = Some("Public Celebrity")).save()
    val order = TestData.newSavedOrder(product = Some(TestData.newSavedProduct(celebrity = Some(celebrity))))
    val email = order.prepareEgraphsSignedEmail()
    email.getFromAddress.getAddress should not be (celebrity.account.email)
    email.getReplyToAddresses.get(0).asInstanceOf[InternetAddress].getAddress should be("noreply@egraphs.com")
  }

  "findByCelebrity" should "find all of a Celebrity's orders by default" in {

    val (will, _, celebrity, product) = newOrderStack

    val (firstOrder, secondOrder, thirdOrder) = (
      will.buy(product).save(),
      will.buy(product).save(),
      will.buy(product).save()
    )

    // Orders of celebrity's products
    val allCelebOrders = orderStore.findByCelebrity(celebrity.id)
    allCelebOrders.toSeq should have length (3)
    allCelebOrders.toSet should be (Set(firstOrder, secondOrder, thirdOrder))
  }

  it should "not find any other Celebrity's orders" in {
    val (will, _, celebrity, product) = newOrderStack
    val (_, _ , _, otherCelebrityProduct) = newOrderStack

    val celebOrder = will.buy(product).save()
    will.buy(otherCelebrityProduct).save()

    val celebOrders = orderStore.findByCelebrity(celebrity.id)

    celebOrders.toSeq should have length(1)
    celebOrders.head should be (celebOrder)
  }

  it should "only find a particular Order when composed with OrderIdFilter" in {
    val (will, _, celebrity, product) = newOrderStack

    val firstOrder = will.buy(product).save()
    will.buy(product).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.orderId(firstOrder.id))

    found.toSeq.length should be (1)
    found.head should be (firstOrder)
  }

  it should "exclude orders that have reviewStatus of PendingAdminReview, RejectedByAdmin, or RejectedByCelebrity when composed with ActionableFilter" in {
    val (will, _, celebrity, product) = newOrderStack
    val actionableOrder = will.buy(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*)
    found.toSeq.length should be(1)
    found.toSet should be(Set(actionableOrder))
  }

  it should "exclude orders that have Published or reviewable Egraphs when composed with ActionableFilter" in {
    val (will, _, celebrity, product) = newOrderStack
    val admin = Administrator().save()

    // Make an order for each Egraph State, and save an Egraph in that state
    val ordersByEgraphState = EgraphState.values.map {
      state =>
        val order = will.buy(product).approveByAdmin(admin).save()
        order.newEgraph.withEgraphState(state).save()
        (state, order)
    }

    // Also buy one without an Egraph
    val orderWithoutEgraph = will.buy(product).approveByAdmin(admin).save()

    // Perform the test
    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*)

    found.toSeq.length should be (2)
    val rejectedByAdminOrder = ordersByEgraphState.find(_._1 == EgraphState.RejectedByAdmin).get._2
    found.toSet should be (Set(
      orderWithoutEgraph,
      rejectedByAdminOrder
    ))
  }

  it should "only include orders that are pendingAdminReview when composed with that filter" in {
    val (will, _, celebrity, product) = newOrderStack
    will.buy(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    val pendingOrder = will.buy(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.pendingAdminReview)
    found.toSeq.length should be(1)
    found.toSet should be(Set(pendingOrder))
  }

  it should "only include orders that are rejected when composed with that filter" in {
    val (will, _, celebrity, product) = newOrderStack
    will.buy(product).withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    will.buy(product).withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
    val rejectedOrder1 = will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByAdmin).save()
    val rejectedOrder2 = will.buy(product).withReviewStatus(OrderReviewStatus.RejectedByCelebrity).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.rejected)
    found.toSeq.length should be(2)
    found.toSet should be(Set(rejectedOrder1, rejectedOrder2))
  }

  "findByFilter" should "restrict by filter but not by celebrity" in {
    val (customer0, product0) = newCustomerAndProduct
    val order0 = customer0.buy(product0).save()
    val (customer1, product1) = newCustomerAndProduct
    val order1 = customer1.buy(product1).save()

    val found = orderStore.findByFilter()
    found.toSeq.length should be(2)
    found.toSet should be(Set(order0, order1))
  }

  "countOrders" should "return count of orders made against InventoryBatches" in {
    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val product1 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product2 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val inventoryBatch1 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val inventoryBatch2 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    inventoryBatch1.products.associate(product1)
    inventoryBatch2.products.associate(product2)
    customer.buy(product1).save()
    customer.buy(product2).save()
    customer.buy(product2).save()

    val inventoryBatchIds = Seq(inventoryBatch1.id, inventoryBatch2.id)
    orderStore.countOrders(inventoryBatchIds) should be(3)
  }

  "isBuyerOrRecipient" should "return true if customer is either buy or recipient" in {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()
    val anotherCustomer = TestData.newSavedCustomer()
    val order = buyer.buy(TestData.newSavedProduct(), recipient=recipient).save()

    order.isBuyerOrRecipient(Some(buyer.id)) should be(true)
    order.isBuyerOrRecipient(Some(recipient.id)) should be(true)
    order.isBuyerOrRecipient(Some(anotherCustomer.id)) should be(false)
    order.isBuyerOrRecipient(None) should be(false)
  }

  //
  // Private methods
  //
  private def newCustomerAndProduct: (Customer, Product) = {
    (TestData.newSavedCustomer(), TestData.newSavedProduct())
  }

  private def newOrderStack = {
    val buyer  = TestData.newSavedCustomer().copy(name="Will Chan").save()
    val recipient = TestData.newSavedCustomer().copy(name="Erem Boto").save()
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity))
    (buyer, recipient, celebrity, product)
  }
}