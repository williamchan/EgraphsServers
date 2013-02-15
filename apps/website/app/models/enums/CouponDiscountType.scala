package models.enums

import egraphs.playutils.Enum

sealed abstract class CouponDiscountType(val name: String)

object CouponDiscountType extends Enum {
  sealed abstract class EnumVal(name: String) extends CouponDiscountType(name) with Value

  val Flat = new EnumVal("Flat"){}
  val Percentage = new EnumVal("Percentage"){}
}

trait HasCouponDiscountType[T] {
  def _discountType: String

  def discountType: CouponDiscountType = {
    CouponDiscountType(_discountType).getOrElse(
      throw new IllegalArgumentException(_discountType)
    )
  }

  def withDiscountType(value: CouponDiscountType): T
}
