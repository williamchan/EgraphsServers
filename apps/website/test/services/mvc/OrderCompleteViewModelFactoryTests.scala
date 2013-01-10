package services.mvc

import utils.EgraphsUnitTest
import models._
import models.enums.CashTransactionType
import java.util
import java.sql.Timestamp
import java.util.Date
import services.Finance.TypeConversions._
import org.joda.money.{CurrencyUnit, Money}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.config.ConfigFileProxy
import utils.TestData
import utils.DBTransactionPerTest

@RunWith(classOf[JUnitRunner])
class OrderCompleteViewModelFactoryTests extends EgraphsUnitTest with DBTransactionPerTest {
  
  "fromOrder" should "use the correct model data" in new EgraphsTestApplication {    
    val order = TestData.newSavedOrder()
    val buyer = order.buyer
    val recipient = order.recipient
    val recipientAccount = recipient.account
    val inventoryBatch = order.inventoryBatch
    val buyerAccount = buyer.account
    val product = order.product
    val celebrity = product.celebrity
    val cashTransaction = CashTransaction(orderId = Some(order.id), accountId = order.buyer.account.id,
        amountInCurrency = product.priceInCurrency).withCashTransactionType(CashTransactionType.EgraphPurchase).save()

    val viewModel = new OrderCompleteViewModelFactory(smartMock[ConfigFileProxy]).fromOrder(order)
    viewModel.buyerEmail should be (buyerAccount.email)
    viewModel.buyerName should be (buyer.name)
    viewModel.celebName should be (celebrity.publicName)
    viewModel.expectedDeliveryDate should be (Order.expectedDeliveryDate(celebrity))
    viewModel.orderDate should be (order.created)
    viewModel.orderNumber should be (order.id)
    viewModel.ownerEmail should be (recipientAccount.email)
    viewModel.ownerName should be (order.recipientName)
    viewModel.productName should be (product.name)
    viewModel.productId should be (product.id)
    viewModel.totalPrice should be (order.amountPaid)
    viewModel.digitalPrice should be (product.price)
    viewModel.printPrice should be (Money.zero(CurrencyUnit.USD))
    viewModel.faqHowLongLink should include("/faq#how-long")
    viewModel.hasPrintOrder should be(false)
    viewModel.withAffiliateMarketing should be(false)
  }
  
  "fromOrder" should "set printPrice and hasPrintOrder when there is an associated PrintOrder" in new EgraphsTestApplication {    
    val order = TestData.newSavedOrder()
    val product = order.product
    val printOrder = PrintOrder(orderId = order.id).save()
    val cashTransaction = CashTransaction(orderId = Some(order.id), accountId = order.buyer.account.id,
        amountInCurrency = BigDecimal(200)).withCashTransactionType(CashTransactionType.EgraphPurchase).save()

    val viewModel = new OrderCompleteViewModelFactory(smartMock[ConfigFileProxy]).fromOrder(order)
    viewModel.totalPrice should be (cashTransaction.cash)
    viewModel.digitalPrice should be (product.price)
    viewModel.printPrice should be (printOrder.amountPaid)
    viewModel.hasPrintOrder should be(true)
  }
  
  "fromOrder" should "set totalPrice to zero if no associated CashTransaction was provided" in new EgraphsTestApplication {    
    val order = TestData.newSavedOrder()
    val product = order.product
    val printOrder = PrintOrder(orderId = order.id).save()

    val viewModel = new OrderCompleteViewModelFactory(smartMock[ConfigFileProxy]).fromOrder(order)
    viewModel.totalPrice should be (Money.zero(CurrencyUnit.USD))
    viewModel.digitalPrice should be (product.price)
    viewModel.printPrice should be (printOrder.amountPaid)
    viewModel.hasPrintOrder should be(true)
  }
}
