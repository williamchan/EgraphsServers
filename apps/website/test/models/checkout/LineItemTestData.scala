package models.checkout

import utils.TestData
import models.{Coupon, Product}
import services.AppConfig
import services.payment.{Payment, YesMaamPayment, StripeTestPayment}

/**
 * Collection of helpers for generating line items and types of various sorts, primarily to use
 * for LineItemTests. Unless named as 'saved' whatever, these are not saved.
 */
object LineItemTestData {
  import services.Finance.TypeConversions._
  import TestData._

  def seqOf[T](gen: => T)(n: Int): Seq[T] = (0 to n).toSeq.map(_ => gen)


  def randomEgraphOrderType(withPrint: Boolean = false, product: Option[Product] = None) = EgraphOrderLineItemType(
    productId = (product getOrElse newSavedProduct()).id,
    recipientName = generateFullname(),
    framedPrint = withPrint
  )
  def randomEgraphOrderItem(withPrint: Boolean = false) = randomEgraphOrderType(withPrint).lineItems(Nil, Nil).get.head


  def randomPrintOrderType = PrintOrderLineItemType(newSavedOrder())
  def randomPrintOrderItem = randomPrintOrderType.lineItems(Nil, Nil).get.head

  def randomGiftCertificateItem = randomGiftCertificateType.lineItems().get.head
  def randomGiftCertificateType = GiftCertificateLineItemType(generateFullname, randomMoney)

  def randomCouponType(coupon: Coupon = newSavedCoupon()) = {
    val couponTypeServices = AppConfig.instance[CouponLineItemTypeServices]
    couponTypeServices.findByCouponCode(coupon.code).get
  }

  def taxItemOn(subtotal: SubtotalLineItem): TaxLineItem = randomTaxType.lineItems(Seq(subtotal), Nil).get.head
  def randomTaxItem: TaxLineItem = taxItemOn(randomSubtotalItem)
  def randomTaxType: TaxLineItemType = TaxLineItemType("98888", randomTaxRate, Some("Test tax"))

  def randomSubtotalItem: SubtotalLineItem = SubtotalLineItem(randomMoney)
  def randomTotalItem = TotalLineItem(randomMoney)
  def randomBalanceItem = BalanceLineItem(randomMoney)

  def randomCashTransactionType = CashTransactionLineItemType.create(Some(stripePayment.testToken().id), zipcode)
  def randomCashTransactionItem = {
    val services = AppConfig.instance[CashTransactionLineItemServices].copy(payment = yesMaamPayment)
    randomCashTransactionType.lineItems(Seq(randomBalanceItem)).get.head.copy(_services = services)
  }

  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()
  def randomTaxRate = BigDecimal(random.nextInt(15).toDouble / 100)


  def newCheckout: FreshCheckout = {
    val buyer = Some(newSavedAccount())
    val address = newSavedAddress(buyer)
    Checkout.create(Seq(randomGiftCertificateType), buyer, address)
  }
  def newSavedCheckout() = newCheckout.insert()
  def newTransactedCheckout() = newCheckout.transact(Some(randomCashTransactionType))

  def stripePayment: StripeTestPayment = bootstrapped[StripeTestPayment]
  def yesMaamPayment: YesMaamPayment = bootstrapped[YesMaamPayment]
  private def bootstrapped[T <: Payment](implicit manifest: Manifest[T]) = {
    val pment = AppConfig.instance[T]; pment.bootstrap(); pment
  }

  protected def zipcode = Some("98888")
}
