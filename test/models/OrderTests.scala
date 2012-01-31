package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import services.Time
import utils._
import services.AppConfig

class OrderTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Order]
  with CreatedUpdatedEntityTests[Order]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val orderStore = AppConfig.instance[OrderStore]
  val orderQueryFilters = AppConfig.instance[OrderQueryFilters]

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
  "An order" should "create eGraphs that are properly configured" in {
    val signature = "derp".getBytes
    val audio = "derp".getBytes

    val eGraph = Order(id=100L).newEgraph

    eGraph.orderId should be (100L)
    eGraph.state should be (EgraphState.AwaitingVerification)
  }

  it should "start out not charged" in {
    Order().paymentState should be (Order.PaymentState.NotCharged)
  }

  it should "update payment state correctly" in {
    Order().withPaymentState(Order.PaymentState.Charged).paymentState should be (Order.PaymentState.Charged)
  }

  it should "serialize the correct Map for the API" in {
    val buyer  = TestData.newSavedCustomer().copy(name="Will Chan").save()
    val recipient = TestData.newSavedCustomer().copy(name="Erem Boto").save()
    val recipientName = "Eremizzle"
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = celebrity.newProduct.save()
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
    val token = TestData.newStripeToken();
    val amount = BigDecimal(100.500000)

    val order = customer.buy(product).copy(stripeCardTokenId=Some(token.getId),  amountPaidInCurrency=amount)
    val charged = order.charge.issueAndSave()

    orderStore.findById(charged.order.id) should not be (None)
    cashTransactionStore.findById(charged.transaction.id) should not be (None)
    charged.order.stripeChargeId should not be (None)
    
  }
  
  "findByCelebrity" should "find all of a Celebrity's orders by default" in {

    val (will, recipient, celebrity, product) = newOrderStack

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

  it should "exclude orders that are Verified or AwaitingVerification when composed with ActionableFilter" in {
    val (will, _, celebrity, product) = newOrderStack

    // Make an order for each Egraph State, and save an Egraph in that state
    val orders = EgraphState.named.map { case (_, state) =>
      val order = will.buy(product).save()
      order
        .newEgraph
        .withState(state)
        .saveWithoutAssets()

      (state, order)
    }

    // Also buy one without an eGraph
    val orderWithoutEgraph = will.buy(product).save()

    // Perform the test
    val found = orderStore.findByCelebrity(celebrity.id, orderQueryFilters.actionableOnly)

    found.toSeq.length should be (5)
    found.toSet should be (Set(
      orderWithoutEgraph,
      orders(EgraphState.RejectedVoice),
      orders(EgraphState.RejectedSignature),
      orders(EgraphState.RejectedBoth),
      orders(EgraphState.RejectedPersonalAudit)
    ))
  }


  //
  // Private methods
  //
  def newCustomerAndProduct: (Customer, Product) = {
    (TestData.newSavedCustomer(), Celebrity().save().newProduct.save())
  }

  def newOrderStack = {
    val buyer  = TestData.newSavedCustomer().copy(name="Will Chan").save()
    val recipient = TestData.newSavedCustomer().copy(name="Erem Boto").save()
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = celebrity.newProduct.save()

    (buyer, recipient, celebrity, product)
  }
}