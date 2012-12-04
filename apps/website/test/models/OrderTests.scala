package models

import enums._
import utils._
import services.{AppConfig, ConsumerApplication}
import org.joda.money.CurrencyUnit
import services.payment.{Charge, NiceCharge}
import javax.mail.internet.InternetAddress
import play.api.test.FakeRequest

class OrderTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Order]
  with CreatedUpdatedEntityTests[Long, Order]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  private def orderStore = AppConfig.instance[OrderStore]
  private def orderQueryFilters = AppConfig.instance[OrderQueryFilters]
  private def cashTransactionStore = AppConfig.instance[CashTransactionStore]
  private def consumerApp = AppConfig.instance[ConsumerApplication]

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
      messageToCelebrity = Some("Wizzle you're the best!"),
      requestedMessage = Some("Happy birthday, Erem!")
    ).withPaymentStatus(PaymentStatus.Charged)
  }

  //
  // Test cases
  //

  "Order" should "require certain fields" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Order().save()}
    exception.getLocalizedMessage should include ("Order: recipientName must be specified")
  }

  "An order" should "create Egraphs that are properly configured" in new EgraphsTestApplication {
    val egraph = Order(id=100L).newEgraph

    egraph.orderId should be (100L)
    egraph.egraphState should be (EgraphState.AwaitingVerification)
  }

  it should "start out not charged" in new EgraphsTestApplication {
    Order().paymentStatus should be (PaymentStatus.NotCharged)
  }

  it should "update payment state correctly" in new EgraphsTestApplication {
    Order().withPaymentStatus(PaymentStatus.Charged).paymentStatus should be (PaymentStatus.Charged)
  }
  
  it should "be non-promotional by default when purchased" in new EgraphsTestApplication {
    val (customer, product) = newCustomerAndProduct
    
    // _orderType not explicitly set so it will take its default value,  
    // which should not be promotional 
    val savedNormalOrder = new Order(
        recipientId = customer.id,
        recipientName = "Rafalca",
        buyerId = customer.id,
        productId = product.id,
        inventoryBatchId = TestData.newSavedInventoryBatch(product).id
      ).save()
    val Some(restoredNormalOrder) = restoreEntity(savedNormalOrder.id)
    
    savedNormalOrder should not be ('Promotional)
    restoredNormalOrder should not be ('Promotional)
  }
  
  it should "save promotional orders as such" in new EgraphsTestApplication {
    val (customer, product) = newCustomerAndProduct
    
    // _orderType set explicitly to promotional
    val savedPromoOrder = new Order(
        recipientId = customer.id,
        recipientName = "Rafalca",
        buyerId = customer.id,
        productId = product.id,
        inventoryBatchId = TestData.newSavedInventoryBatch(product).id,
        _orderType = OrderType.Promotional.name
      ).save()
    val Some(restoredPromoOrder) = restoreEntity(savedPromoOrder.id)
    
    savedPromoOrder should be ('Promotional)
    restoredPromoOrder should be ('Promotional)
  }
  

  "renderedForApi" should "serialize the correct Map for the API" in new EgraphsTestApplication {
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
    rendered("audioPrompt") should be("Recipient: " + order.recipientName)
    rendered("orderType") should be(order.writtenMessageRequest.name)
    rendered.contains("created") should be(true)
    rendered.contains("updated") should be(true)
    rendered.contains("product") should be(true)
  }

  "approveByAdmin" should "change reviewStatus to ApprovedByAdmin" in new EgraphsTestApplication {
    val order = newEntity.save()
    order.reviewStatus should be (OrderReviewStatus.PendingAdminReview)
    intercept[IllegalArgumentException] {order.approveByAdmin(null)}
    val admin = Administrator().save()
    order.approveByAdmin(admin).save().reviewStatus should be (OrderReviewStatus.ApprovedByAdmin)
  }

  "rejectByAdmin" should "change reviewStatus to RejectedByAdmin" in new EgraphsTestApplication {
    val order = newEntity.save()
    order.reviewStatus should be (OrderReviewStatus.PendingAdminReview)
    order.rejectionReason should be (None)

    intercept[IllegalArgumentException] {order.rejectByAdmin(null)}

    val rejectedOrder = order.rejectByAdmin(Administrator().save(), Some("It made me cry")).save()
    rejectedOrder.reviewStatus should be (OrderReviewStatus.RejectedByAdmin)
    rejectedOrder.rejectionReason.get should be ("It made me cry")
  }

  "rejectByCelebrity" should "change reviewStatus to RejectedByCelebrity" in new EgraphsTestApplication {
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

  "withChargeInfo" should "set the PaymentStatus, store stripe info, and create an associated CashTransaction" in new EgraphsTestApplication {
    val (will, _, _, product) = TestData.newSavedOrderStack()
    val order = will.buy(product).withPaymentStatus(PaymentStatus.Charged).save()
    CashTransaction(accountId = will.account.id, orderId = Some(order.id), stripeCardTokenId = Some("mytoken"), stripeChargeId = Some(NiceCharge.id),billingPostalCode = Some("55555"))
      .withCash(product.price).withCashTransactionType(CashTransactionType.EgraphPurchase).save()

    // verify PaymentStatus
    order.paymentStatus should be(PaymentStatus.Charged)

    // verify CashTransaction

    val cashTransaction = cashTransactionStore.findByOrderId(order.id).head
    cashTransaction.accountId should be(will.account.id)
    cashTransaction.orderId should be(Some(order.id))
    cashTransaction.amountInCurrency should be(product.priceInCurrency)
    cashTransaction.currencyCode should be(CurrencyUnit.USD.getCode)
    cashTransaction.cashTransactionType should be(CashTransactionType.EgraphPurchase)
    cashTransaction.stripeCardTokenId.get should be("mytoken")
    cashTransaction.stripeChargeId.get should include ("test charge against services.payment.YesMaamPayment")

  }

  "refund" should "refund the Stripe charge, change the PaymentStatus to Refunded, and create a refund CashTransaction" in new EgraphsTestApplication {
    val (will, _, _, product) = TestData.newSavedOrderStack()
    val order = will.buy(product).withPaymentStatus(PaymentStatus.Charged).save()
    CashTransaction(accountId = will.account.id, orderId = Some(order.id), stripeCardTokenId = Some("mytoken"), stripeChargeId = Some(NiceCharge.id),billingPostalCode = Some("55555"))
      .withCash(product.price).withCashTransactionType(CashTransactionType.EgraphPurchase).save()

    val (_, refundCharge: Charge) = order.refund()
    orderStore.get(order.id).paymentStatus should be(PaymentStatus.Refunded)
    refundCharge.refunded should be(true)

    val cashTransactions = cashTransactionStore.findByOrderId(order.id)
    cashTransactions.length should be(2)
    val purchaseTxn = cashTransactions.find(b => b.cashTransactionType == CashTransactionType.EgraphPurchase).head
    purchaseTxn.accountId should be(will.account.id)
    purchaseTxn.orderId should be(Some(order.id))
    purchaseTxn.amountInCurrency should be(BigDecimal(product.price.getAmount))
    purchaseTxn.currencyCode should be(CurrencyUnit.USD.getCode)
    val refundTxn = cashTransactions.find(b => b.cashTransactionType == CashTransactionType.PurchaseRefund).head
    refundTxn.accountId should be(will.account.id)
    refundTxn.orderId should be(Some(order.id))
    refundTxn.amountInCurrency should be(BigDecimal(product.price.negated().getAmount))
    refundTxn.currencyCode should be(CurrencyUnit.USD.getCode)
  }

  "prepareEgraphsSignedEmail" should "not use celebrity's email" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val order = TestData.newSavedOrder(product = Some(TestData.newSavedProduct(celebrity = Some(celebrity))))
    implicit val request = FakeRequest()
    val (email, _, _) = order.prepareEgraphSignedEmail
    email.getFromAddress.getAddress should not be (celebrity.account.email)
    email.getReplyToAddresses.get(0).asInstanceOf[InternetAddress].getAddress should be("noreply@egraphs.com")
  }

  "isBuyerOrRecipient" should "return true if customer is either buy or recipient" in new EgraphsTestApplication {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()
    val anotherCustomer = TestData.newSavedCustomer()
    val order = buyer.buy(TestData.newSavedProduct(), recipient=recipient).save()

    order.isBuyerOrRecipient(Some(buyer.id)) should be(true)
    order.isBuyerOrRecipient(Some(recipient.id)) should be(true)
    order.isBuyerOrRecipient(Some(anotherCustomer.id)) should be(false)
    order.isBuyerOrRecipient(None) should be(false)
  }

  "GalleryOrderFactory" should "create PendingEgraphViewModels from orders" in new EgraphsTestApplication {
    val (buyer, recipient, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()
    celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()

    val order1 = buyer.buy(product, recipient=recipient).save()
    val order2 = recipient.buy(product, recipient=recipient).save()

    val results = orderStore.galleryOrdersWithEgraphs(recipient.id)

    val pendingViews = GalleryOrderFactory.makePendingEgraphViewModel(results)

    pendingViews.size should be (2)

    val orderIds = pendingViews.map(pendingView => pendingView.orderId)
    orderIds should contain (order1.id)
    orderIds should contain (order2.id)
  }

  "redactedName" should "redact properly" in new EgraphsTestApplication {
    Order(recipientName="Herp Derpson-Schiller").redactedRecipientName should be ("Herp D.S.")
    Order(recipientName="Herp").redactedRecipientName should be ("Herp")
    Order(recipientName="Herp Derpson bin-Hoffberger").redactedRecipientName should be ("Herp D.b.H.")
    Order(recipientName="Herp D").redactedRecipientName should be ("Herp D.")

    for (herpDerpson <- List("Herp D", "Herp Derpson", "Herp Derpson ", " Herp Derpson", "Herp  Derpson")) {
      Order(recipientName=herpDerpson).redactedRecipientName should be ("Herp D.")
    }
  }


  "GalleryOrderFactory" should "create FulfilledEgraphViewModels from orders" in new EgraphsTestApplication {
    val (buyer, recipient, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()
    celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()

    val order = buyer.buy(product, recipient=recipient).save()
    val order1 = buyer.buy(product, recipient=recipient).save()
    val egraph = TestData.newSavedEgraph()

    val results = List((order, Option(egraph)), (order, None))

    implicit val request = FakeRequest()
    val fulfilledViews = GalleryOrderFactory.makeFulfilledEgraphViewModel(results, "fakeappid", consumerApp)(request)
    //create an egraph

    fulfilledViews.size should be (2)
    fulfilledViews.toList(0).get.orderId should be (order.id)
    //unfulfilled egraph should not have created a view model
    fulfilledViews.toList(1).isEmpty should be (true)
  }
  
  "filterPendingOrders" should "return filtered results with user displayable egraphs" in new EgraphsTestApplication {
    val (buyer, recipient, celebrity, product) = TestData.newSavedOrderStack()
    val admin = Administrator().save()
    celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()

    val order = buyer.buy(product, recipient=recipient).save()

    val egraph = order.newEgraph.save()

    TestData.newSavedEgraph(Some(order)).withEgraphState(EgraphState.FailedBiometrics).save()
    TestData.newSavedEgraph(Some(order)).withEgraphState(EgraphState.RejectedByAdmin).save()
    TestData.newSavedEgraph(Some(order)).withEgraphState(EgraphState.Published).save()

    val results = GalleryOrderFactory.filterPendingOrders(orderStore.galleryOrdersWithEgraphs(recipient.id).toList)

    results.size should be (1)

    val queriedEgraph = results.head._2.get

    queriedEgraph.id should be (egraph.id)
  }

  //
  // Private methods
  //
  private def newCustomerAndProduct: (Customer, Product) = {
    (TestData.newSavedCustomer(), TestData.newSavedProduct())
  }
}
