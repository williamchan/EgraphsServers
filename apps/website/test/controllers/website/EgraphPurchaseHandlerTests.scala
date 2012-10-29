package controllers.website

import utils.{ClearsCacheAndBlobsAndValidationBefore, TestData, EgraphsUnitTest}
import models.enums.{CashTransactionType, PrintingOption, WrittenMessageRequest}
import services.Finance.TypeConversions._
import services.AppConfig
import services.payment.StripeTestPayment
import services.db.{DBSession, TransactionSerializable}
import models.FailedPurchaseDataStore
import models.{PrintOrderStore, CashTransactionStore, Order, OrderStore}
import services.http.forms.purchase.CheckoutShippingForm
import org.joda.money.{CurrencyUnit, Money}
import play.api.test.FakeRequest
import com.stripe.Stripe
import services.http.EgraphsSession
import models.Celebrity
import models.Product
import models.Order

class EgraphPurchaseHandlerTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {

  private def db = AppConfig.instance[DBSession]
  private def orderStore = AppConfig.instance[OrderStore]
  private def cashTransactionStore = AppConfig.instance[CashTransactionStore]
  private def printOrderStore = AppConfig.instance[PrintOrderStore]
  private def failedPurchaseDataStore = AppConfig.instance[FailedPurchaseDataStore]

  private def payment = {
    val instance = AppConfig.instance[StripeTestPayment]
    instance.bootstrap()
    
    instance
  }

  private val recipientName = "My Recipient"
  private val recipientEmail = TestData.generateEmail(prefix = "recipient")
  private val buyerName = "My Buyer"
  private val buyerEmail = TestData.generateEmail(prefix = "buyer")

  "An EgraphPurchaseHAndler" should "create Order and CashTransaction for a digital-only purchase" in new EgraphsTestApplication {    
    val Right(orderFromHandler) = executePurchaseHandler()
    db.connected(TransactionSerializable) {
      val order = orderStore.get(orderFromHandler.id)
      order.amountPaidInCurrency should be(BigDecimal(50))
      order.recipientName should be(recipientName)
      order.buyer.name should be(buyerName)

      val cashTransaction = cashTransactionStore.findByOrderId(order.id).head
      cashTransaction.orderId should be(Some(order.id))
      cashTransaction.stripeCardTokenId should not be(None)
      cashTransaction.stripeChargeId should not be(None)
      cashTransaction.amountInCurrency should be(BigDecimal(50))
      cashTransaction.billingPostalCode should be(Some("55555"))
      cashTransaction.currencyCode should be(CurrencyUnit.USD.getCode)
      cashTransaction.cashTransactionType should be(CashTransactionType.EgraphPurchase)
    }
  }

  it should "create Order and PrintOrder and CashTransaction with totalAmountPaid if PrintingOption is HighQualityPrint" in new EgraphsTestApplication {
    val shippingForm = CheckoutShippingForm.Valid(
      name = "Egraphs",
      addressLine1 = "615 2nd Ave",
      addressLine2 = Some("300"),
      city = "Seattle",
      state = "WA",
      postalCode = "98102"
    )

    val Right(orderFromHandler) = executePurchaseHandler(
      totalAmountPaid = BigDecimal(95).toMoney(),
      printingOption = PrintingOption.HighQualityPrint,
      shippingForm = Some(shippingForm)
    )

    db.connected(TransactionSerializable) {
      val order = orderStore.get(orderFromHandler.id)
      order.amountPaidInCurrency should be(BigDecimal(50))

      val printOrder = printOrderStore.findByOrderId(order.id).head
      printOrder.shippingAddress should be("Egraphs,615 2nd Ave,300,Seattle,WA,98102")
      printOrder.amountPaidInCurrency should be(BigDecimal(45))

      val cashTransaction = cashTransactionStore.findByOrderId(order.id).head
      cashTransaction.printOrderId should be(Some(printOrder.id))
      cashTransaction.amountInCurrency should be(BigDecimal(95))
    }
  }

  it should "fail to save order when there is insufficient inventory" in {
    val celebAndProduct = db.connected(TransactionSerializable) {
      val product = TestData.newSavedProduct().copy(priceInCurrency = BigDecimal(50))
      product.inventoryBatches.head.copy(numInventory = 0).save()
      (product.celebrity, product)
    }
    
    val result = executePurchaseHandler(
      totalAmountPaid = BigDecimal(50).toMoney(),
      printingOption = PrintingOption.DoNotPrint,
      shippingForm = None,
      celebrityAndProduct = Some(celebAndProduct)
    )

    val failedPurchaseDataFromHandler = result.fold(
      error => error.failedPurchaseData,
      order => throw new Exception("Purchase should have failed, thus there should be no order.")
    )

    db.connected(TransactionSerializable) {
      val failedPurchaseData = failedPurchaseDataStore.get(failedPurchaseDataFromHandler.id)
      failedPurchaseData.errorDescription should startWith("Must have available inventory to purchase product")
      failedPurchaseData.purchaseData should include(recipientName)
      failedPurchaseData.purchaseData should include(recipientEmail)
      failedPurchaseData.purchaseData should include(buyerName)
      failedPurchaseData.purchaseData should include(buyerEmail)
      failedPurchaseData.purchaseData should include("\"productId\":" + celebAndProduct._2.id)
    }
  }

  private def executePurchaseHandler(
    totalAmountPaid: Money = BigDecimal(50).toMoney(),
    printingOption: PrintingOption = PrintingOption.DoNotPrint,
    shippingForm: Option[CheckoutShippingForm.Valid] = None,
    celebrityAndProduct: Option[(Celebrity, Product)] = None
  ): Either[EgraphPurchaseHandler.PurchaseFailed, Order] =
  {
    val (celebrity, product) = celebrityAndProduct.getOrElse {
      db.connected(TransactionSerializable) {
        val product = TestData.newSavedProduct()
        (product.celebrity, product)
      }
    }
    
    val req = FakeRequest().withSession(EgraphsSession.SESSION_ID_KEY -> "12345")

    val purchaseHandler = EgraphPurchaseHandler(
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      buyerName = buyerName,
      buyerEmail = buyerEmail,
      stripeTokenId = payment.testToken().id,
      desiredText = None,
      personalNote = None,
      celebrity = celebrity,
      product = product,
      totalAmountPaid = totalAmountPaid,
      billingPostalCode = "55555",
      flash = req.flash,
      printingOption = printingOption,
      shippingForm = shippingForm,
      writtenMessageRequest = WrittenMessageRequest.SpecificMessage
    )(req)

    purchaseHandler.performPurchase()
  }
}
