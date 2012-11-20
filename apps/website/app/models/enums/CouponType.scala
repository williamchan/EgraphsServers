package models.enums

import services.Utils
import egraphs.playutils.Enum

object CouponType extends Enum {
  sealed trait EnumVal extends Value

  val Promotion = new EnumVal {
    val name = "Promotion"
  }
//  val Prepaid = new EnumVal {
//    val name = "Prepaid"
//  }
//  val Invoiceable = new EnumVal {
//    val name = "Invoiceable"
//  }
}

trait HasCouponType[T] {
  def _couponType: String

  def couponType: CouponType.EnumVal = {
    CouponType(_couponType).getOrElse(
      throw new IllegalArgumentException(_couponType)
    )
  }

  def withCouponType(value: CouponType.EnumVal): T
}
