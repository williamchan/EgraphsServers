package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import libs.Time
import org.squeryl.PrimitiveTypeMode._

class OrderTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Order]
  with CreatedUpdatedEntityTests[Order]
  with ClearsDatabaseAndValidationAfter
{

  //
  // SavingEntityTests[Order] methods
  //
  override def newEntity = {
    val (customer, product) = newCustomerAndProduct

    customer.order(product)
  }

  override def saveEntity(toSave: Order) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    Order.findById(id)
  }

  override def transformEntity(toTransform: Order) = {
    val (customer, product) = newCustomerAndProduct
    val order = customer.order(product)
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

    val eGraph = Order(id=100L).newEgraph(signature, audio)

    eGraph.orderId should be (100L)
    eGraph.signature.toSeq should be (signature.toSeq)
    eGraph.audio.toSeq should be (audio.toSeq)
    eGraph.state should be (AwaitingVerification)
  }

  it should "serialize the correct Map for the API" in {
    val buyer  = Customer(name="Will Chan").save()
    val recipient = Customer(name="Erem Boto").save()
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = celebrity.newProduct.save()
    val order = buyer
      .order(product, recipient)
      .copy(
        messageToCelebrity=Some("toCeleb"),
        requestedMessage=Some("please write this"))

    val rendered = order.renderedForApi

    rendered("id") should be (order.id)
    rendered("product.id") should be (product.id)
    rendered("buyer.id") should be (buyer.id)
    rendered("buyer.name") should be (buyer.name)
    rendered("recipient.id") should be (recipient.id)
    rendered("amountPaidInCents") should be (order.amountPaidInCents)
    rendered("requestedMessage") should be (order.requestedMessage.get)
    rendered("messageToCelebrity") should be (order.messageToCelebrity.get)
    rendered("created") should be (Time.toApiFormat(order.created))
    rendered("updated") should be (Time.toApiFormat(order.updated))
  }
  
  "FindByCelebrity" should "find all of a Celebrity's orders by default" in {

    inTransaction {
      val (will, recipient, celebrity, product) = newOrderStack

      val (firstOrder, secondOrder, thirdOrder) = (
        will.order(product).save(),
        will.order(product).save(),
        will.order(product).save()
      )

      // Orders of celebrity's products
      val allCelebOrders = Order.findByCelebrity(celebrity.id)
      allCelebOrders.toSeq should have length (3)
      allCelebOrders.toSet should be (Set(firstOrder, secondOrder, thirdOrder))
    }
  }

  it should "not find any other Celebrity's orders" in {
    inTransaction {
      val (will, _, celebrity, product) = newOrderStack
      val (_, _ , _, otherCelebrityProduct) = newOrderStack

      val celebOrder = will.order(product).save()
      val otherCelebOrder = will.order(otherCelebrityProduct).save()

      val celebOrders = Order.findByCelebrity(celebrity.id)

      celebOrders.toSeq should have length(1)
      celebOrders.head should be (celebOrder)
    }
  }

  it should "only find a particular Order when composed with OrderIdFilter" in {
    inTransaction {
      val (will, _, celebrity, product) = newOrderStack

      val firstOrder = will.order(product).save()
      val secondOrder = will.order(product).save()

      val found = Order.findByCelebrity(celebrity.id, OrderIdFilter(firstOrder.id))

      found.toSeq.length should be (1)
      found.head should be (firstOrder)
    }
  }

  it should "exclude orders that are Verified or AwaitingVerification when composed with ActionableFilter" in {
    inTransaction {
      val (will, _, celebrity, product) = newOrderStack

      // Make an order for each Egraph State, and save an Egraph in that state
      val orders = Egraph.states.map { case (_, state) =>
        val order = will.order(product).save()
        val egraph = order
          .newEgraph("herp".getBytes, "derp".getBytes)
          .withState(state)
          .save()

        (state, order)
      }

      // Also order one without an eGraph
      val orderWithoutEgraph = will.order(product).save()

      // Perform the test
      val found = Order.findByCelebrity(celebrity.id, ActionableFilter)

      found.toSeq.length should be (4)
      found.toSet should be (Set(
        orderWithoutEgraph,
        orders(RejectedVocals),
        orders(RejectedHandwriting),
        orders(RejectedPersonalAudit)
      ))

    }
  }

  //
  // Private methods
  //
  def newCustomerAndProduct: (Customer, Product) = {
    (Customer().save(), Celebrity().save().newProduct.save())
  }

  def newOrderStack = {
    val buyer  = Customer(name="Will Chan").save()
    val recipient = Customer(name="Erem Boto").save()
    val celebrity = Celebrity(firstName=Some("George"), lastName=Some("Martin")).save()
    val product = celebrity.newProduct.save()

    (buyer, recipient, celebrity, product)
  }
}