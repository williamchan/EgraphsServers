package services.mvc

import utils.EgraphsUnitTest
import models._
import models.Order
import java.util
import java.sql.Timestamp
import java.util.Date

class OrderCompleteViewModelFactoriesTests extends EgraphsUnitTest {
  "fromModels" should "use the correct model data" in {
    // Set up the domain models
    val celeb = Celebrity(publicName = "Joe Celebrity")
    val product = Product(name = "Supergreat Product")
    val buyer = Customer(name="Joe Buyer")
    val buyerAccount = Account(email="joebuyer@egraphs.com")
    val recipientAccount = Account(email="joerecipient@egraphs.com")
    val order = Order(
      amountPaidInCurrency=100.00,
      created = new Timestamp(new Date().getTime),
      recipientName = "Joe Recipient"
    )

    val inventoryBatch = mock[InventoryBatch]
    val expectedDate = new util.Date()
    inventoryBatch.getExpectedDate returns expectedDate

    // Generate the viewmodel
    val viewModel = new OrderCompleteViewModelFactory().fromModels(
      celeb, product, buyer, buyerAccount, recipientAccount, order, inventoryBatch
    )

    // Check expectations
    viewModel.buyerEmail should be (buyerAccount.email)
    viewModel.buyerName should be (buyer.name)
    viewModel.celebName should be (celeb.publicName)
    viewModel.guaranteedDeliveryDate should be (expectedDate)
    viewModel.orderDate should be (order.created)
    viewModel.orderNumber should be (order.id)
    viewModel.ownerEmail should be (recipientAccount.email)
    viewModel.ownerName should be (order.recipientName)
    viewModel.productName should be (product.name)
    viewModel.totalPrice should be (order.amountPaid)
  }

  // TODO: implement this test
  "fromOrder" should "pass the correct models when delegating to fromModels" in (pending)

}
