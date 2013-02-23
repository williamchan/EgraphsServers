package models.checkout

import utils.{TestData, DBTransactionPerTest, DateShouldMatchers, EgraphsUnitTest}
import models.checkout.checkout.Conversions._
import models.enums._
import services.AppConfig
import scala.Some


class CouponLineItemTests extends EgraphsUnitTest
  with LineItemTests[CouponLineItemType, CouponLineItem]
  with SavesAsLineItemEntityThroughServicesTests[CouponLineItem, CouponLineItemServices]
  with CheckoutTestCases
  with DateShouldMatchers
  with DBTransactionPerTest
{
  import TestData._
  import LineItemTestData._
  import CheckoutScenario._
  import RichCheckoutConversions._
  import ScenarioPredicates._

  //
  // LineItemTests members
  //
  override lazy val newItemType: CouponLineItemType = randomCouponType()

  override def resolvableItemSets: Seq[LineItems] = Seq(
    Seq(randomSubtotalItem),
    Seq(randomGiftCertificateItem, randomSubtotalItem)
  )

  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(SubtotalLineItemType),
    Seq(SubtotalLineItemType, randomGiftCertificateType)
  )

  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(TotalLineItemType),
    Seq(TotalLineItemType, randomTaxType, BalanceLineItemType)
  )

  override def restoreLineItem(id: Long) = lineItemStore.findByIdWithCodeType(id, CouponLineItemType.codeType)

  override def hasExpectedRestoredDomainObject(lineItem: CouponLineItem) = {
    import CouponUsageType._
    val coupon = lineItem.domainObject

    coupon.usageType match {
      case Unlimited => coupon.isActive
      case Prepaid => coupon.discountAmount < newItemType.coupon.discountAmount
      case OneUse => !coupon.isActive
      case unknown => throw new IllegalStateException(s"What is a ${unknown}?")
    }
  }

  lazy val itemServices = AppConfig.instance[CouponLineItemServices]


  //
  // CheckoutTestCases members
  //
  override lazy val scenarios = Seq(
    orderAndCouponScenario,
    orderAndCouponThenGiftCertScenario
  )

  def orderAndCouponScenario = CheckoutScenario(orderAndCouponSeq)
  def orderAndCouponThenGiftCertScenario = CheckoutScenario(orderAndCouponSeq, Seq(randomGiftCertificateType))

  def orderAndCoupon = {
    val product = newSavedProduct()
    val order = randomEgraphOrderType(product = Some(product))
    val coupon = randomCouponType(newSavedCoupon(maxAmount = product.price.getAmount))
    (order, coupon)
  }

  def orderAndCouponSeq: LineItemTypes = {
    val (order, coupon) = orderAndCoupon
    Seq(order, coupon)
  }


  "A coupon" should "reduce the total of a checkout" in {
    import LineItemMatchers._
    val (orderType, couponType) = orderAndCoupon
    val withoutCoupon = CheckoutScenario(Seq(orderType)).initialCheckout.withZipcode(Some("11111"))
    val withCoupon = withoutCoupon.withAdditionalTypes(Seq(couponType))
    val couponItem = withCoupon.coupons.head
    val totalDiff = withCoupon.total.amount minus withoutCoupon.total.amount

    couponItem should haveAmount(totalDiff)
  }
}
