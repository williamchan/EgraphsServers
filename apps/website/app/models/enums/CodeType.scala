package models.enums

import egraphs.playutils.Enum

sealed abstract class CodeType(val name: String)

object CodeType extends Enum {
  sealed abstract class EnumVal(name: String) extends CodeType(name) with Value

  val StripeCharge = new EnumVal("StripeChargeLineItemType"){}
  val GiftCertificate = new EnumVal("GiftCertificateLineItemType"){}

}
