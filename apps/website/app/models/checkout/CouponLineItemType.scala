package models.checkout

import models.Coupon
import org.joda.money.{Money, CurrencyUnit}


case class CouponLineItemType(couponCode: String) extends LineItemType[Coupon] {
  import models.checkout.checkout.Conversions._
  import models.enums.{LineItemNature, CheckoutCodeType}

  override def toJson = ""
  override def description = ""
  override def id = 0L
  override def nature = LineItemNature.Discount
  override def codeType = CheckoutCodeType.Coupon // wrong code type
  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = Some{ Seq(
    CouponLineItem(this, Money.zero(CurrencyUnit.USD))
  )}
}
