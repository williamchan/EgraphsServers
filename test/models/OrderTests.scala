package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import libs.Time

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

  it should "herp" in {
    import org.squeryl.PrimitiveTypeMode._
    inTransaction {
      Order.findByCelebrity(1L)
    }
  }

  it should "find all of a Celebrity's orders" in {
    import org.squeryl.PrimitiveTypeMode._
    inTransaction {
      val (will, _, celebrity, product) = newOrderStack
      val (_, _ , otherCelebrity, otherCelebrityProduct) = newOrderStack

      val otherProduct = celebrity.newProduct.save()
      val orderToFulfill = will.order(product).save()
      val egraph = orderToFulfill.newEgraph("herp".getBytes, "derp".getBytes).withState(Verified).save()

      val fulfilledOrder = orderToFulfill.copy(verifiedEgraphId = Some(egraph.id)).save()
      val productOrder = will.order(product).save()
      val otherProductOrder = will.order(otherProduct).save()
      val otherCelebrityProductOrder = will.order(otherCelebrityProduct).save()


      // Orders of celebrity's products
      val allCelebOrders = Order.findByCelebrity(celebrity.id)
      allCelebOrders.toSeq should have length (3)
      allCelebOrders.toSet should be (Set(fulfilledOrder, productOrder, otherProductOrder))

      // Order of celebrity's unfulfilled products
      val unfulfilledCelebOrders = Order.findByCelebrity(celebrity.id, UnfulfilledFilter)
      unfulfilledCelebOrders.toSeq should have length (2)
      unfulfilledCelebOrders.toSet should be (Set(productOrder, otherProductOrder))

      // A particular orderId of a celebrity's
      val firstProductOrder = Order.findByCelebrity(celebrity.id, OrderIdFilter(productOrder.id))
      firstProductOrder.toSeq should have length(1)
      firstProductOrder.toSeq.apply(0) should be (productOrder)

      // Querying a celebrity's order for an orderId belonging to another celeb should return none
      val orderOfOtherCelebrity = Order.findByCelebrity(otherCelebrity.id, OrderIdFilter(productOrder.id))
      orderOfOtherCelebrity.toSeq should have length (0)
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