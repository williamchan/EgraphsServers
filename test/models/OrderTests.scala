package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}

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
      personalizedMessage = Some("Happy birthday, Erem!")
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

  //
  // Private methods
  //
  def newCustomerAndProduct: (Customer, Product) = {
    (Customer().save(), Celebrity().save().newProduct.save())
  }
}