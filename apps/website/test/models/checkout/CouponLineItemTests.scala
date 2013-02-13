package models.checkout

import utils.{DBTransactionPerTest, DateShouldMatchers, EgraphsUnitTest}
import models.checkout.checkout.Conversions._
import models.enums._
import services.AppConfig


class CouponLineItemTests extends EgraphsUnitTest
  with LineItemTests[CouponLineItemType, CouponLineItem]
  with SavesAsLineItemEntityThroughServicesTests[CouponLineItem, CouponLineItemServices]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  import LineItemTestData._

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
    }
  }

  lazy val itemServices = AppConfig.instance[CouponLineItemServices]
}
