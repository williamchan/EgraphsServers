package models.enums

import egraphs.playutils.Enum

sealed abstract class CodeType(val name: String)

object CodeType extends Enum {
  sealed abstract class EnumVal(name: String) extends CodeType(name) with Value

  //
  // Products
  //

  //
  // Discounts
  //
  val GiftCertificate = new EnumVal("GiftCertificateLineItemType"){}

  //
  // Charges
  //
  val StripeCharge = new EnumVal("StripeChargeLineItemType"){}

  //
  // Summaries
  //
  val Subtotal = new EnumVal("SubtotalLineItemType"){}
  val Total = new EnumVal("TotalLineItemType"){}

  //
  // Taxes
  //
  val Tax = new EnumVal("TaxLineItemType"){}
}
