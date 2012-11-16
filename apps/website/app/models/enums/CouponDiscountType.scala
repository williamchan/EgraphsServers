package models.enums

import services.Utils

object CouponDiscountType extends Utils.Enum {
  sealed trait EnumVal extends Value

  val Flat = new EnumVal {
    val name = "Flat"
  }
  val Percentage = new EnumVal {
    val name = "Percentage"
  }
}

trait HasCouponDiscountType[T] {
  def _discountType: String

  def discountType: CouponDiscountType.EnumVal = {
    CouponDiscountType(_discountType).getOrElse(
      throw new IllegalArgumentException(_discountType)
    )
  }

  def withDiscountType(value: CouponDiscountType.EnumVal): T
}
