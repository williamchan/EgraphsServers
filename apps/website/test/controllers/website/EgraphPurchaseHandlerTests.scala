package controllers.website

import utils.{ClearsDatabaseAndValidationBefore, TestData, EgraphsUnitTest}
import controllers.website.PostBuyProductEndpoint.EgraphPurchaseHandler
import models.enums.{WrittenMessageRequest, PrintingOption}
import services.Finance.TypeConversions._
import services.AppConfig
import services.payment.StripeTestPayment
import services.db.TransactionSerializable
import models.{CashTransactionStore, OrderStore}

class EgraphPurchaseHandlerTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore {

  private val orderStore = AppConfig.instance[OrderStore]
  private val cashTransactionStore = AppConfig.instance[CashTransactionStore]

  private val payment = AppConfig.instance[StripeTestPayment]
  payment.bootstrap()

  private val recipientName = "My Recipient"
  private val recipientEmail = "recipient@egraphs.com"
  private val buyerName = "My Buyer"
  private val buyerEmail = "buyer@egraphs.com"

  it should "create Order and CashTransaction with price (not Product.Price)" in {
    val (celebrity, product) = db.connected(TransactionSerializable) {
      val product = TestData.newSavedProduct().copy(priceInCurrency = BigDecimal(50))
      (product.celebrity, product)
    }

    val purchaseHandler: EgraphPurchaseHandler = EgraphPurchaseHandler(
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      buyerName = buyerName,
      buyerEmail = buyerEmail,
      stripeTokenId = payment.testToken().id,
      desiredText = None,
      personalNote = None,
      celebrity = celebrity,
      product = product,
      price = BigDecimal(95).toMoney(),
      billingPostalCode = "55555",
      printingOption = PrintingOption.DoNotPrint,
      shippingForm = None,
      writtenMessageRequest = WrittenMessageRequest.SpecificMessage
    )
    purchaseHandler.execute()

    db.connected(TransactionSerializable) {
      // CashTransaction is a local record of the Stripe charge. If it is correct, then the Stripe charge should be correct.
      cashTransactionStore.get(1).amountInCurrency should be(BigDecimal(95))
      orderStore.get(1).amountPaid should be(BigDecimal(95).toMoney())
    }
  }

}
