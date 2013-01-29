package models.enums

import services.Utils
import egraphs.playutils.Enum

sealed abstract class CouponType(val name: String)

object CouponType extends Enum {
  sealed abstract class EnumVal(name: String) extends CouponType(name) with Value

  val Promotion = new EnumVal("Promotion"){}
  val GiftCertificate = new EnumVal("GiftCertificate"){}

//  val Prepaid = new EnumVal("Prepaid"){}
//  val Invoiceable = new EnumVal("Invoiceable"){}
}

trait HasCouponType[T] {
  def _couponType: String

  def couponType: CouponType = {
    CouponType(_couponType).getOrElse(
      throw new IllegalArgumentException(_couponType)
    )
  }

  def withCouponType(value: CouponType): T
}
