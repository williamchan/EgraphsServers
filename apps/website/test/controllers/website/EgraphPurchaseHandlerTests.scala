package controllers.website

import utils.{ClearsDatabaseAndValidationBefore, TestData, EgraphsUnitTest}
import controllers.website.PostBuyProductEndpoint.EgraphPurchaseHandler
import models.enums.{PrintingOption, WrittenMessageRequest}
import services.Finance.TypeConversions._
import services.AppConfig
import services.payment.StripeTestPayment
import services.db.{DBSession, TransactionSerializable}
import models.{Product, PrintOrderStore, CashTransactionStore, OrderStore}
import services.http.forms.purchase.CheckoutShippingForm
import org.joda.money.Money

class EgraphPurchaseHandlerTests extends EgraphsUnitTest with ClearsDatabaseAndValidationBefore {

  private val db = AppConfig.instance[DBSession]
  private val orderStore = AppConfig.instance[OrderStore]
  private val cashTransactionStore = AppConfig.instance[CashTransactionStore]
  private val printOrderStore = AppConfig.instance[PrintOrderStore]

  private val payment = AppConfig.instance[StripeTestPayment]
  payment.bootstrap()

  private val recipientName = "My Recipient"
  private val recipientEmail = "recipient@egraphs.com"
  private val buyerName = "My Buyer"
  private val buyerEmail = "buyer@egraphs.com"

  it should "create Order and CashTransaction with price (not Product.Price)" in {
    executePurchaseHandler(price = BigDecimal(95).toMoney())
    db.connected(TransactionSerializable) {
      // CashTransaction is a local record of the Stripe charge. If it is correct, then the Stripe charge should be correct.
      cashTransactionStore.get(1).amountInCurrency should be(BigDecimal(95))
      orderStore.get(1).amountPaid should be(BigDecimal(95).toMoney())
    }
  }

  it should "persist PrintOrder is PrintingOption is HighQualityPrint" in {

    val shippingForm = CheckoutShippingForm.Valid(
      name = "Egraphs",
      addressLine1 = "615 2nd Ave",
      addressLine2 = Some("300"),
      city = "Seattle",
      state = "WA",
      postalCode = "98102",
      email = ""
    )
    executePurchaseHandler(
      price = Product.defaultPrice.toMoney(),
      printingOption = PrintingOption.HighQualityPrint,
      shippingForm = Some(shippingForm))

    db.connected(TransactionSerializable) {
      val printOrder = printOrderStore.get(1)
      printOrder.orderId should be(1L)
      printOrder.quantity should be(1)
      printOrder.shippingAddress should be("Egraphs, 615 2nd Ave, 300, Seattle, WA 98102")
    }
  }

  private def executePurchaseHandler(price: Money,
                                     printingOption: PrintingOption = PrintingOption.DoNotPrint,
                                     shippingForm: Option[CheckoutShippingForm.Valid] = None) {
    val (celebrity, product) = db.connected(TransactionSerializable) {
      val product = TestData.newSavedProduct()
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
      price = price,
      billingPostalCode = "55555",
      printingOption = printingOption,
      shippingForm = shippingForm,
      writtenMessageRequest = WrittenMessageRequest.SpecificMessage
    )
    purchaseHandler.execute()
  }
}
