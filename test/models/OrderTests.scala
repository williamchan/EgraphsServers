package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import services.Time
import utils._
import services.AppConfig
import services.payment.Payment
import models.Egraph.EgraphState

class OrderTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Order]
  with CreatedUpdatedEntityTests[Order]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  private val orderStore = AppConfig.instance[OrderStore]
  private val payment = AppConfig.instance[Payment]
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
      transactionId = Some(12345),
      recipientId = order.recipientId,
      recipientName = "Derpy Jones",
      stripeCardTokenId = Some("12345"),
      stripeChargeId = Some("12345"),
      messageToCelebrity = Some("Wizzle you're the best!"),
      requestedMessage = Some("Happy birthday, Erem!")
    ).withPaymentState(Order.PaymentState.Charged)
  }

  //
  // Test cases
  //
  "An order" should "create Egraphs that are properly configured" in {
    val egraph = Order(id=100L).newEgraph

    egraph.orderId should be (100L)
    egraph.state should be (EgraphState.AwaitingVerification)
  }

  it should "start out not charged" in {
    Order().paymentState should be (Order.PaymentState.NotCharged)
  }

  it should "update payment state correctly" in {
    Order().withPaymentState(Order.PaymentState.Charged).paymentState should be (Order.PaymentState.Charged)
  }

  "approveByAdmin" should "change reviewStatus to ApprovedByAdmin" in {
    val order = newEntity.save()
    order.reviewStatus should be (Order.ReviewStatus.PendingAdminReview.stateValue)
    intercept[IllegalArgumentException] {order.approveByAdmin(null)}
    val admin = Administrator().save()
    order.approveByAdmin(admin).save().reviewStatus should be (Order.ReviewStatus.ApprovedByAdmin.stateValue)
  }

  "rejectByAdmin" should "change reviewStatus to RejectedByAdmin" in {
    val order = newEntity.save()
    order.reviewStatus should be (Order.ReviewStatus.PendingAdminReview.stateValue)
    order.rejectionReason should be (None)

    intercept[IllegalArgumentException] {order.rejectByAdmin(null)}

    val rejectedOrder = order.rejectByAdmin(Administrator().save(), Some("It made me cry")).save()
    rejectedOrder.reviewStatus should be (Order.ReviewStatus.RejectedByAdmin.stateValue)
    rejectedOrder.rejectionReason.get should be ("It made me cry")
  }

  "rejectByCelebrity" should "change reviewStatus to RejectedByCelebrity" in {
    val order = newEntity.copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue).save()
    order.reviewStatus should be (Order.ReviewStatus.ApprovedByAdmin.stateValue)
    order.rejectionReason should be (None)

    intercept[IllegalArgumentException] {newEntity.save().rejectByCelebrity(null)}
    intercept[IllegalArgumentException] {order.rejectByCelebrity(null)}
    intercept[IllegalArgumentException] {order.rejectByCelebrity(Celebrity())}

    val rejectedOrder = order.rejectByCelebrity(order.product.celebrity, Some("It made me cry")).save()
    rejectedOrder.reviewStatus should be (Order.ReviewStatus.RejectedByCelebrity.stateValue)
    rejectedOrder.rejectionReason.get should be ("It made me cry")
  }

  it should "serialize the correct Map for the API" in {
    val buyer  = TestData.newSavedCustomer().copy(name="Will Chan").save()
    val recipient = TestData.newSavedCustomer().copy(name="Erem Boto").save()
    val recipientName = "Eremizzle"
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity))
    val order = buyer
      .buy(product, recipient)
      .copy(
        messageToCelebrity=Some("toCeleb"),
        requestedMessage=Some("please write this"),
        recipientName=recipientName)

    val rendered = order.renderedForApi

    rendered("product") should be (product.renderedForApi)
    rendered("id") should be (order.id)
    rendered("buyerId") should be (buyer.id)
    rendered("buyerName") should be (buyer.name)
    rendered("recipientId") should be (recipient.id)
    rendered("recipientName") should be (recipientName)
    rendered("amountPaidInCents") should be (order.amountPaid.getAmountMinor)
    rendered("reviewStatus") should be (order.reviewStatus)
    rendered("requestedMessage") should be (order.requestedMessage.get)
    rendered("messageToCelebrity") should be (order.messageToCelebrity.get)
    rendered("created") should be (Time.toApiFormat(order.created))
    rendered("updated") should be (Time.toApiFormat(order.updated))
  }

  "charge" should "fail to charge if stripe token is unavailable" in {
    evaluating { Order(stripeCardTokenId=None).charge } should produce [IllegalArgumentException]
  }

  it should "create a properly configured OrderCharge" in {
    val stripeToken = "mytoken"
    val buyerId = 1L
    val amount = BigDecimal(100.50)
    val order = Order(stripeCardTokenId=Some(stripeToken), amountPaidInCurrency=amount, buyerId=buyerId)
    val orderCharge = order.charge

    orderCharge.order.paymentState should be (Order.PaymentState.Charged)
    orderCharge.transaction.cash should be (order.amountPaid)
    orderCharge.transaction.accountId should be (buyerId)
    orderCharge.stripeCardTokenId should be (stripeToken)
  }

  "An OrderCharge" should "perform the correct charge" in {
    val cashTransactionStore = AppConfig.instance[CashTransactionStore]
    val customer = TestData.newSavedCustomer()
    val product  = TestData.newSavedProduct()
    val token = payment.testToken
    val amount = BigDecimal(100.500000)

    val order = customer.buy(product).copy(stripeCardTokenId=Some(token.id),  amountPaidInCurrency=amount)
    val charged = order.charge.issueAndSave()

    orderStore.findById(charged.order.id) should not be (None)
    cashTransactionStore.findById(charged.transaction.id) should not be (None)
    charged.order.stripeChargeId should not be (None)

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
    val actionableOrder = will.buy(product).copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue).save()
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.PendingAdminReview.stateValue).save()
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.RejectedByAdmin.stateValue).save()
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.RejectedByCelebrity.stateValue).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*)
    found.toSeq.length should be(1)
    found.toSet should be(Set(actionableOrder))
  }

  it should "exclude orders that have Published or reviewable Egraphs when composed with ActionableFilter" in {
    val (will, _, celebrity, product) = newOrderStack
    val admin = Administrator().save()

    // Make an order for each Egraph State, and save an Egraph in that state
    val orders = EgraphState.all.map {
      case (_, state) =>
        val order = will.buy(product).approveByAdmin(admin).save()
        order
          .newEgraph
          .withState(state)
          .saveWithoutAssets()
        (state, order)
    }

    // Also buy one without an Egraph
    val orderWithoutEgraph = will.buy(product).approveByAdmin(admin).save()

    // Perform the test
    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly: _*)

    found.toSeq.length should be (2)
    found.toSet should be (Set(
      orderWithoutEgraph,
      orders(EgraphState.RejectedByAdmin)
    ))
  }

  it should "only include orders that are pendingAdminReview when composed with that filter" in {
    val (will, _, celebrity, product) = newOrderStack
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue).save()
    val pendingOrder = will.buy(product).copy(reviewStatus = Order.ReviewStatus.PendingAdminReview.stateValue).save()
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.RejectedByAdmin.stateValue).save()
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.RejectedByCelebrity.stateValue).save()

    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.pendingAdminReview)
    found.toSeq.length should be(1)
    found.toSet should be(Set(pendingOrder))
  }

  it should "only include orders that are rejected when composed with that filter" in {
    val (will, _, celebrity, product) = newOrderStack
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue).save()
    will.buy(product).copy(reviewStatus = Order.ReviewStatus.PendingAdminReview.stateValue).save()
    val rejectedOrder1 = will.buy(product).copy(reviewStatus = Order.ReviewStatus.RejectedByAdmin.stateValue).save()
    val rejectedOrder2 = will.buy(product).copy(reviewStatus = Order.ReviewStatus.RejectedByCelebrity.stateValue).save()

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