package models

import enums.OrderReviewStatus
import utils._
import services.{Utils, Time, AppConfig}
import exception.InsufficientInventoryException

class CustomerTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Customer]
  with CreatedUpdatedEntityTests[Long, Customer]
  with DBTransactionPerTest
{
  val customerStore = AppConfig.instance[CustomerStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Customer(name = "customer", username = "username")
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
  "Customer" should "require certain fields" in {
    val exception = intercept[IllegalArgumentException] {Customer().save()}
    exception.getLocalizedMessage should include("Customer: name must be specified")
  }

  "A customer" should "produce Orders that are properly configured" in {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val order = buyer.buy(product, recipient=recipient)

    order.buyerId should be (buyer.id)
    order.recipientId should be (recipient.id)
    order.productId should be (product.id)
    order.amountPaid should be (product.price)
  }

  it should "make itself the recipient if no recipient is specified" in {
    val (buyer, _, product) = savedBuyerRecipientAndProduct()

    buyer.buy(product).recipientId should be (buyer.id)
  }

  "buy" should "set inventoryBatchId on the Order" in {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val order = buyer.buy(product, recipient = recipient).save()
    order.inventoryBatchId should be(product.inventoryBatches.head.id)
  }

  "buy" should "create an order whose approval status depends on play config's adminreview.skip" in {
    // Set up
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val buyerWithAdminSkip = buyer.copy(
      services=buyer.services.copy(playConfig=Utils.properties("adminreview.skip" -> "true"))
    )

    // Run tests
    buyer.buy(product, recipient=recipient).reviewStatus should be (OrderReviewStatus.PendingAdminReview)
    buyerWithAdminSkip.buy(product, recipient=recipient).reviewStatus should be (OrderReviewStatus.ApprovedByAdmin)
  }

  "buy" should "throw InsufficientInventoryException if no inventory is available" in {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val inventoryBatch = product.inventoryBatches.head.copy(numInventory = 1).save()

    // first one should pass
    buyer.buy(product, recipient = recipient).save()

    // should fail due to lack of inventory
    val exception0 = intercept[InsufficientInventoryException] {
      buyer.buy(product, recipient = recipient).save()
    }
    exception0.getLocalizedMessage should include("Must have available inventory to purchase product")

    // should fail due to lack of active InventoryBatch
    inventoryBatch.copy(numInventory = 10, startDate = TestData.jan_01_2012, endDate = TestData.jan_01_2012).save()
    val exception1 = intercept[InsufficientInventoryException] {
      buyer.buy(product, recipient = recipient).save()
    }
    exception1.getLocalizedMessage should include("Must have available inventory to purchase product")
  }

  "findOrCreateByEmail" should "find or create as appropriate" in {
    val acct = Account(email = "customer-" + Time.toBlobstoreFormat(Time.now) + "@egraphs.com").save()
    acct.customerId should be(None)
    val customer = customerStore.findOrCreateByEmail(acct.email, "joe fan")

    val updatedAcct = acct.services.accountStore.get(acct.id)
    customer.id should be(updatedAcct.customerId.get)
    customerStore.findOrCreateByEmail(acct.email, "joe fan") should be(customer)
  }

  private def savedBuyerRecipientAndProduct(): (Customer, Customer, Product) = {
    (TestData.newSavedCustomer(), TestData.newSavedCustomer(), TestData.newSavedProduct())
  }
}