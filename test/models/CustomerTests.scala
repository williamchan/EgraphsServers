package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests, TestData}
import services.{Time, AppConfig}
import exception.InsufficientInventoryException

class CustomerTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Customer]
  with CreatedUpdatedEntityTests[Customer]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val customerStore = AppConfig.instance[CustomerStore]

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
    customerStore.findById(id)
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
    val product = TestData.newSavedProduct()

    val order = buyer.buy(product, recipient=recipient)

    order.buyerId should be (buyer.id)
    order.recipientId should be (recipient.id)
    order.productId should be (product.id)
    order.amountPaid should be (product.price)
  }

  it should "make itself the recipient if no recipient is specified" in {
    val buyer = TestData.newSavedCustomer()
    val product = TestData.newSavedProduct()

    buyer.buy(product).recipientId should be (buyer.id)
  }

  "buy" should "set inventoryBatchId on the Order" in {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()
    val product = TestData.newSavedProduct()
    val order = buyer.buy(product, recipient = recipient).save()
    order.inventoryBatchId should be(product.inventoryBatches.head.id)
  }

  "buy" should "throw InsufficientInventoryException if no inventory is available" in {
    val buyer = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()
    val product = TestData.newSavedProduct()
    val inventoryBatch = product.inventoryBatches.head.copy(numInventory = 1).save()

    // first one should pass
    buyer.buy(product, recipient = recipient).save()

    // should fail due to lack of inventory
    val exception0 = intercept[InsufficientInventoryException] {
      buyer.buy(product, recipient = recipient).save()
    }
    exception0.getLocalizedMessage.contains("Must have available inventory to purchase product") should be(true)

    // should fail due to lack of active InventoryBatch
    inventoryBatch.copy(numInventory = 10, startDate = TestData.jan_01_2012, endDate = TestData.jan_01_2012).save()
    val exception1 = intercept[InsufficientInventoryException] {
      buyer.buy(product, recipient = recipient).save()
    }
    exception1.getLocalizedMessage.contains("Must have available inventory to purchase product") should be(true)
  }

  "findOrCreateByEmail" should "find or create as appropriate" in {
    val acct = Account(email = "customer-" + Time.toBlobstoreFormat(Time.now) + "@egraphs.com").save()
    acct.customerId should be(None)
    val customer = customerStore.findOrCreateByEmail(acct.email)

    val updatedAcct = acct.services.accountStore.findById(acct.id).get
    customer.id should be(updatedAcct.customerId.get)
    customerStore.findOrCreateByEmail(acct.email) should be(customer)
  }
}