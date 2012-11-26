package models.enums

import services.Utils
import egraphs.playutils.Enum

object CouponUsageType extends Enum {
  sealed trait EnumVal extends Value

  val OneUse = new EnumVal {
    val name = "OneUse"
  }
  val Unlimited = new EnumVal {
    val name = "Unlimited"
  }
}

trait HasCouponUsageType[T] {
  def _usageType: String

  def usageType: CouponUsageType.EnumVal = {
    CouponUsageType(_usageType).getOrElse(
      throw new IllegalArgumentException(_usageType)
    )
  }

  def withUsageType(value: CouponUsageType.EnumVal): T
}
