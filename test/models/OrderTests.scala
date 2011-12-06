package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import libs.Time
import Order.FindByCelebrity.Filters
import utils._

class OrderTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Order]
  with CreatedUpdatedEntityTests[Order]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{

  //
  // SavingEntityTests[Order] methods
  //
  override def newEntity = {
    val (customer, product) = newCustomerAndProduct

    customer.buy(product).order
  }

  override def saveEntity(toSave: Order) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    Order.findById(id)
  }

  override def transformEntity(toTransform: Order) = {
    val (customer, product) = newCustomerAndProduct
    val order = customer.buy(product).order
    toTransform.copy(
      productId = order.productId,
      buyerId = order.buyerId,
      recipientId = order.recipientId,
      messageToCelebrity = Some("Wizzle you're the best!"),
      requestedMessage = Some("Happy birthday, Erem!")
    )
  }

  //
  // Test cases
  //
  "An order" should "create eGraphs that are properly configured" in {
    val signature = "herp".getBytes
    val audio = "derp".getBytes

    val eGraph = Order(id=100L).newEgraph

    eGraph.orderId should be (100L)
    eGraph.state should be (AwaitingVerification)
  }

  it should "serialize the correct Map for the API" in {
    val buyer  = TestData.newSavedCustomer().copy(name="Will Chan").save()
    val recipient = TestData.newSavedCustomer().copy(name="Erem Boto").save()
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = celebrity.newProduct.save()
    val order = buyer
      .buy(product, recipient)
      .order
      .copy(
        messageToCelebrity=Some("toCeleb"),
        requestedMessage=Some("please write this"))

    val rendered = order.renderedForApi

    rendered("id") should be (order.id)
    rendered("productId") should be (product.id)
    rendered("buyerId") should be (buyer.id)
    rendered("buyerName") should be (buyer.name)
    rendered("recipientId") should be (recipient.id)
    rendered("amountPaidInCents") should be (order.amountPaid.getAmountMinor)
    rendered("requestedMessage") should be (order.requestedMessage.get)
    rendered("messageToCelebrity") should be (order.messageToCelebrity.get)
    rendered("created") should be (Time.toApiFormat(order.created))
    rendered("updated") should be (Time.toApiFormat(order.updated))
  }
  
  "FindByCelebrity" should "find all of a Celebrity's orders by default" in {

    val (will, recipient, celebrity, product) = newOrderStack

    val (firstOrder, secondOrder, thirdOrder) = (
      will.buy(product).save().order,
      will.buy(product).save().order,
      will.buy(product).save().order
    )

    // Orders of celebrity's products
    val allCelebOrders = Order.FindByCelebrity(celebrity.id)
    allCelebOrders.toSeq should have length (3)
    allCelebOrders.toSet should be (Set(firstOrder, secondOrder, thirdOrder))
  }

  it should "not find any other Celebrity's orders" in {
    val (will, _, celebrity, product) = newOrderStack
    val (_, _ , _, otherCelebrityProduct) = newOrderStack

    val celebOrder = will.buy(product).save().order
    will.buy(otherCelebrityProduct).save()

    val celebOrders = Order.FindByCelebrity(celebrity.id)

    celebOrders.toSeq should have length(1)
    celebOrders.head should be (celebOrder)
  }

  it should "only find a particular Order when composed with OrderIdFilter" in {
    val (will, _, celebrity, product) = newOrderStack

    val firstOrder = will.buy(product).save().order
    will.buy(product).save().order

    val found = Order.FindByCelebrity(celebrity.id, Filters.OrderId(firstOrder.id))

    found.toSeq.length should be (1)
    found.head should be (firstOrder)
  }

  it should "exclude orders that are Verified or AwaitingVerification when composed with ActionableFilter" in {
    val (will, _, celebrity, product) = newOrderStack

    // Make an buy for each Egraph State, and save an Egraph in that state
    val orders = Egraph.states.map { case (_, state) =>
      val order = will.buy(product).save().order
      order
        .newEgraph
        .withState(state)
        .saveWithoutAssets()

      (state, order)
    }

    // Also buy one without an eGraph
    val orderWithoutEgraph = will.buy(product).save().order

    // Perform the test
    val found = Order.FindByCelebrity(celebrity.id, Filters.ActionableOnly)

    found.toSeq.length should be (4)
    found.toSet should be (Set(
      orderWithoutEgraph,
      orders(RejectedVocals),
      orders(RejectedHandwriting),
      orders(RejectedPersonalAudit)
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