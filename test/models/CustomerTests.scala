package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import utils.{ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}

class CustomerTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Customer]
  with CreatedUpdatedEntityTests[Customer]
  with ClearsDatabaseAndValidationAfter
{
  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Customer()
  }

  override def saveEntity(toSave: Customer) = {
    toSave.save
  }

  override def restoreEntity(id: Long) = {
    Customer.findById(id)
  }

  override def transformEntity(toTransform: Customer) = {
    toTransform.copy(
      name = Some("name")
    )
  }

  //
  // Test cases
  //
  "A customer" should "produce Orders that are properly configured" in {
    val buyer = Customer(id=1L)
    val recipient = Customer(id=2L)
    val product = Product(id=1L)

    val order = buyer.order(product, recipient=recipient)

    order.buyerId should be (buyer.id)
    order.recipientId should be (recipient.id)
    order.productId should be (product.id)
  }

  it should "make itself the recipient if no recipient is specified" in {
    val buyer = Customer(id=1L)

    buyer.order(Product()).recipientId should be (buyer.id)
  }

}