package controllers.website

import com.stripe.Stripe
import models._
import models.enums._
import org.joda.money.{CurrencyUnit, Money}
import play.api.test.FakeRequest
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import services.Finance.TypeConversions._
import services.payment.StripeTestPayment
import models.FailedPurchaseDataStore
import services.http.EgraphsSession
import services.http.forms.purchase.CheckoutShippingForm
import utils.{ClearsCacheAndBlobsAndValidationBefore, TestData, EgraphsUnitTest}

class EgraphPurchaseHandlerTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {

  private def db = AppConfig.instance[DBSession]
  private def orderStore = AppConfig.instance[OrderStore]
  private def cashTransactionStore = AppConfig.instance[CashTransactionStore]
  private def couponStore = AppConfig.instance[CouponStore]
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
  
  /**
   * EgraphPurchaseHandler charges totalAmountPaid. The coupon should have already been applied by the time 
   * totalAmountPaid is specified.
   */
  it should "charge totalAmountPaid and correctly mark one-use coupon as used" in new EgraphsTestApplication {
    val coupon = db.connected(TransactionSerializable) { Coupon(discountAmount = BigDecimal(10)).save() }
    val Right(orderFromHandler) = executePurchaseHandler(coupon = Some(coupon), totalAmountPaid = BigDecimal(40).toMoney())
    db.connected(TransactionSerializable) {
      val order = orderStore.get(orderFromHandler.id)
      order.amountPaidInCurrency should be(BigDecimal(50))

      val cashTransaction = cashTransactionStore.findByOrderId(order.id).head
      cashTransaction.orderId should be(Some(order.id))
      cashTransaction.amountInCurrency should be(BigDecimal(40))
      
      couponStore.findByCode(coupon.code).head.isActive should be(false)
    }
  }
  
  it should "not create a CashTransaction if totalAmountPaid was zero" in new EgraphsTestApplication {
    val coupon = db.connected(TransactionSerializable) { Coupon(discountAmount = BigDecimal(50)).save() }
    val Right(orderFromHandler) = executePurchaseHandler(coupon = Some(coupon), totalAmountPaid = BigDecimal(0).toMoney(), 
        stripeTokenId = None)
    db.connected(TransactionSerializable) {
      val order = orderStore.get(orderFromHandler.id)
      order.amountPaidInCurrency should be(BigDecimal(50))

      cashTransactionStore.findByOrderId(order.id).headOption should be(None)
    }
  }
  
  it should "throw exception if totalAmountPaid was positive but no stripeTokenId was specified" in new EgraphsTestApplication {
    intercept[Exception] { executePurchaseHandler(stripeTokenId = None) }
  }
  
  // ======================================================= private helpers

  private def executePurchaseHandler(
    totalAmountPaid: Money = BigDecimal(50).toMoney(),
    printingOption: PrintingOption = PrintingOption.DoNotPrint,
    shippingForm: Option[CheckoutShippingForm.Valid] = None,
    celebrityAndProduct: Option[(Celebrity, Product)] = None,
    stripeTokenId: Option[String] = Some(payment.testToken().id),
    coupon: Option[Coupon] = None
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
      stripeTokenId = stripeTokenId,
      desiredText = None,
      personalNote = None,
      celebrity = celebrity,
      product = product,
      totalAmountPaid = totalAmountPaid,
      coupon = coupon,
      billingPostalCode = "55555",
      flash = req.flash,
      printingOption = printingOption,
      shippingForm = shippingForm,
      writtenMessageRequest = WrittenMessageRequest.SpecificMessage
    )(req)

    purchaseHandler.performPurchase()
  }
}
