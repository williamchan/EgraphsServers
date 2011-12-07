package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests, TestData}

class CustomerTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Customer]
  with CreatedUpdatedEntityTests[Customer]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Customer()
  }

  override def saveEntity(toSave: Customer) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    Customer.findById(id)
  }

  override def transformEntity(toTransform: Customer) = {
    toTransform.copy(
      name = "name"
    )
  }

  //
  // Test cases
  //
  "A customer" should "produce Orders that are properly configured" in {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()
    val product = Product(id=1L)

    val order = buyer.buy(product, recipient=recipient)

    order.buyerId should be (buyer.id)
    order.recipientId should be (recipient.id)
    order.productId should be (product.id)
    order.amountPaid should be (product.price)
  }

  it should "make itself the recipient if no recipient is specified" in {
    val buyer = TestData.newSavedCustomer()

    buyer.buy(Product()).recipientId should be (buyer.id)
  }
}