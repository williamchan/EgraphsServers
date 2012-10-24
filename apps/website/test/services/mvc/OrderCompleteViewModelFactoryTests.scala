package services.mvc

import utils.EgraphsUnitTest
import models._
import models.Order
import java.util
import java.sql.Timestamp
import java.util.Date
import services.Finance.TypeConversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.config.ConfigFileProxy
import utils.TestData
import utils.DBTransactionPerTest

@RunWith(classOf[JUnitRunner])
class OrderCompleteViewModelFactoryTests extends EgraphsUnitTest with DBTransactionPerTest {
  
  "fromOrders" should "use the correct model data" in new EgraphsTestApplication {    
    // Set up domain models as stubs / spies / mocks
    val order = TestData.newSavedOrder()
    val buyer = order.buyer
    val recipient = order.recipient
    val recipientAccount = recipient.account
    val inventoryBatch = order.inventoryBatch
    val buyerAccount = buyer.account
    val product = order.product
    val celeb = product.celebrity
    val cashTransaction = order.services.cashTransactionStore.findByOrderId(order.id).headOption
    val hasPrintOrder = order.services.printOrderStore.findByOrderId(order.id).headOption.isDefined

/*    val product = smartMock[Product]
    product.celebrity returns celeb

    val buyerAccount = Account(email="joebuyer@egraphs.com")
    val buyer = smartMock[Customer]
    buyer.name returns "Joe Buyer"
    buyer.account returns buyerAccount

    val recipient = smartMock[Customer]
    val recipientAccount = Account(email="joerecipient@egraphs.com")
    recipient.account returns recipientAccount

    val inventoryBatch = smartMock[InventoryBatch]
    val expectedDate = new util.Date()
    inventoryBatch.getExpectedDate returns expectedDate

    val order = smartMock[Order]
    order.id returns 1
    order.amountPaid returns BigDecimal(100.00).toMoney() // totalPrice should ignore this and instead use the value from CashTransaction
    order.product returns product
    order.buyer returns buyer
    order.created returns new Timestamp(new Date().getTime)
    order.recipientName returns "Joe Recipient"
    order.recipient returns recipient
    order.inventoryBatch returns inventoryBatch
    val orderServices = smartMock[OrderServices]
    order.services returns orderServices
    val printOrderStore = smartMock[PrintOrderStore]
    orderServices.printOrderStore returns printOrderStore
    printOrderStore.findByOrderId(1) returns List(mock[PrintOrder])
    val cashTransactionStore = smartMock[CashTransactionStore]
    orderServices.cashTransactionStore returns cashTransactionStore
    val cashTransaction = smartMock[CashTransaction]
    cashTransactionStore.findByOrderId(1) returns List(cashTransaction)
    cashTransaction.cash returns BigDecimal(200.00).toMoney()*/

    // Generate the viewmodel from the domain models
    val viewModel = new OrderCompleteViewModelFactory(smartMock[ConfigFileProxy]).fromOrder(order)
    // Check expectations
    viewModel.buyerEmail should be (buyerAccount.email)
    viewModel.buyerName should be (buyer.name)
    viewModel.celebName should be (celeb.publicName)
    viewModel.expectedDeliveryDate should be (inventoryBatch.getExpectedDate)
    viewModel.orderDate should be (order.created)
    viewModel.orderNumber should be (order.id)
    viewModel.ownerEmail should be (recipientAccount.email)
    viewModel.ownerName should be (order.recipientName)
    viewModel.productName should be (product.name)
    viewModel.totalPrice should be (order.amountPaid)
    viewModel.faqHowLongLink should include("/faq#how-long")
    viewModel.hasPrintOrder should be(hasPrintOrder)
    viewModel.withAffiliateMarketing should be(false)
  }

}
