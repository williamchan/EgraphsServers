package services.mvc

import utils.EgraphsUnitTest
import models._
import models.Order
import java.util
import java.sql.Timestamp
import java.util.Date
import services.Finance.TypeConversions._

class OrderCompleteViewModelFactoryTests extends EgraphsUnitTest {
  "fromOrders" should "use the correct model data" in {
    // Set up domain models as stubs / spies / mocks
    val celeb = mock[Celebrity]
    celeb.publicName returns "Joe Celebrity"

    val product = mock[Product]
    product.name returns "Supergreat Product"
    product.celebrity returns celeb

    val buyerAccount = Account(email="joebuyer@egraphs.com")
    val buyer = mock[Customer]
    buyer.name returns "Joe Buyer"
    buyer.account returns buyerAccount

    val recipient = mock[Customer]
    val recipientAccount = Account(email="joerecipient@egraphs.com")
    recipient.account returns recipientAccount

    val inventoryBatch = mock[InventoryBatch]
    val expectedDate = new util.Date()
    inventoryBatch.getExpectedDate returns expectedDate

    val order = mock[Order]
    order.id returns 1
    order.amountPaid returns BigDecimal(100.00).toMoney()
    order.product returns product
    order.buyer returns buyer
    order.created returns new Timestamp(new Date().getTime)
    order.recipientName returns "Joe Recipient"
    order.recipient returns recipient
    order.inventoryBatch returns inventoryBatch
    val orderServices = mock[OrderServices]
    order.services returns orderServices
    val printOrderStore = mock[PrintOrderStore]
    orderServices.printOrderStore returns printOrderStore
    printOrderStore.findByOrderId(1) returns List(mock[PrintOrder])

    // Generate the viewmodel from the domain models
    val viewModel = new OrderCompleteViewModelFactory().fromOrder(order)

    // Check expectations
    viewModel.buyerEmail should be (buyerAccount.email)
    viewModel.buyerName should be (buyer.name)
    viewModel.celebName should be (celeb.publicName)
    viewModel.expectedDeliveryDate should be (expectedDate)
    viewModel.orderDate should be (order.created)
    viewModel.orderNumber should be (order.id)
    viewModel.ownerEmail should be (recipientAccount.email)
    viewModel.ownerName should be (order.recipientName)
    viewModel.productName should be (product.name)
    viewModel.totalPrice should be (order.amountPaid)
    viewModel.faqHowLongLink should include("/faq#how-long")
    viewModel.hasPrintOrder should be(true)
    viewModel.withAffiliateMarketing should be(false)
  }

}
