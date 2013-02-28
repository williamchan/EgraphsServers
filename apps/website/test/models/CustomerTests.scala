package models

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.joda.time.DateTimeConstants
import enums.OrderReviewStatus
import exception.InsufficientInventoryException
import services.config.ConfigFileProxy
import services.AppConfig
import utils.ClearsCacheBefore
import utils.CreatedUpdatedEntityTests
import utils.DBTransactionPerTest
import utils.DateShouldMatchers
import utils.EgraphsUnitTest
import utils.SavingEntityIdLongTests
import utils.TestData
import org.joda.time.DateTime

@RunWith(classOf[JUnitRunner])
class CustomerTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[Customer]
  with CreatedUpdatedEntityTests[Long, Customer]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def customerStore = AppConfig.instance[CustomerStore]
  def usernameHistoryStore = AppConfig.instance[UsernameHistoryStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Customer(name = "customer", username = TestData.generateUsername())
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
  "Customer" should "require certain fields" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Customer().save()}
    exception.getLocalizedMessage should include("Customer: name must be specified")
  }

  "A customer" should "produce Orders that are properly configured" in new EgraphsTestApplication {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val order = buyer.buyUnsafe(product, recipient=recipient)

    order.buyerId should be (buyer.id)
    order.recipientId should be (recipient.id)
    order.productId should be (product.id)
    order.amountPaid should be (product.price)
  }

  it should "make itself the recipient if no recipient is specified" in new EgraphsTestApplication {
    val (buyer, _, product) = savedBuyerRecipientAndProduct()

    buyer.buyUnsafe(product).recipientId should be (buyer.id)
  }

  "buy" should "set inventoryBatchId on the Order" in new EgraphsTestApplication {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val order = buyer.buyUnsafe(product, recipient = recipient).save()
    order.inventoryBatchId should be(product.inventoryBatches.head.id)
  }

  it should "create an order whose approval status depends on play config's adminreview.skip" in new EgraphsTestApplication {
    // Set up
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()
    
    val mockConfig = mock[ConfigFileProxy]
    mockConfig.adminreviewSkip returns true
    
    val buyerWithAdminSkip = buyer.copy(services=buyer.services.copy(config=mockConfig))

    buyer.buyUnsafe(product, recipient=recipient).reviewStatus should be (OrderReviewStatus.PendingAdminReview)
    buyerWithAdminSkip.buyUnsafe(product, recipient=recipient).reviewStatus should be (OrderReviewStatus.ApprovedByAdmin)
  }

  it should "create an order whose expected date is relative to the celebrity's expected delay" in new EgraphsTestApplication {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()
    // change expected delay to 5 days
    val delayDays = 5
    product.celebrity.copy(expectedOrderDelayInMinutes = delayDays * DateTimeConstants.MINUTES_PER_DAY).save()

    val order = buyer.buyUnsafe(product, recipient = recipient).save()
    val delayDay = DateTime.now.plusDays(delayDays + 1).toDateMidnight.toDate // rounds up
    order.expectedDate.getTime should be (delayDay.getTime)
  }

  it should "throw InsufficientInventoryException if no inventory is available" in new EgraphsTestApplication {
    val (buyer, recipient, product) = savedBuyerRecipientAndProduct()

    val inventoryBatch = product.inventoryBatches.head.copy(numInventory = 1).save()

    // first one should pass
    buyer.buyUnsafe(product, recipient = recipient).save()

    // should fail due to lack of inventory
    val exception0 = intercept[InsufficientInventoryException] {
      buyer.buyUnsafe(product, recipient = recipient).save()
    }
    exception0.getLocalizedMessage should include("Must have available inventory to purchase product")

    // should fail due to lack of active InventoryBatch
    inventoryBatch.copy(numInventory = 10, startDate = TestData.jan_01_2012, endDate = TestData.jan_01_2012).save()
    val exception1 = intercept[InsufficientInventoryException] {
      buyer.buyUnsafe(product, recipient = recipient).save()
    }
    exception1.getLocalizedMessage should include("Must have available inventory to purchase product")
  }

  private def savedBuyerRecipientAndProduct(): (Customer, Customer, Product) = {
    (TestData.newSavedCustomer(), TestData.newSavedCustomer(), TestData.newSavedProduct())
  }

  "findOrCreateByEmail" should "find or create as appropriate" in new EgraphsTestApplication {
    val (customer, account) = createCustomerWithFindOrCreateByEmail()

    val updatedAcct = account.services.accountStore.get(account.id)
    customer.id should be(updatedAcct.customerId.get)
    customerStore.findOrCreateByEmail(account.email, "joe fan") should be(customer)
  }

  it should "create an username when it creates." in new EgraphsTestApplication {
    val (customer, _) = createCustomerWithFindOrCreateByEmail()
    usernameHistoryStore.findCurrentByCustomer(customer) should not be (None)
  }

  "createByEmail" should "use preexisting customer if available" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    customerStore.createByEmail(customer.account.email, "fananas") should be(customer)
  }

  private def createCustomerWithFindOrCreateByEmail(): (Customer, Account) = {
    val account = TestData.newSavedAccount()
    account.customerId should be(None)
    val customer = customerStore.findOrCreateByEmail(account.email, "joe fan")
    (customer, account)
  }
}