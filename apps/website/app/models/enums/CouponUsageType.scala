package models.enums

import services.Utils
import egraphs.playutils.Enum

sealed abstract class CouponUsageType(val name: String)

object CouponUsageType extends Enum {
  sealed abstract class EnumVal(name: String) extends CouponUsageType(name) with Value

  val OneUse = new EnumVal("OneUse"){}

  val Unlimited = new EnumVal("Unlimited"){}

  /** prepaid amount; e.g. a gift certificate  */
  val Prepaid = new EnumVal("Prepaid"){}
}

trait HasCouponUsageType[T] {
  def _usageType: String

  def usageType: CouponUsageType = {
    CouponUsageType(_usageType).getOrElse(
      throw new IllegalArgumentException(_usageType)
    )
  }

  def withUsageType(value: CouponUsageType): T
}
