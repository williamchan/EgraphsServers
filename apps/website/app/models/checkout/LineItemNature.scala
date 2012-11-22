package models.checkout

import egraphs.playutils.Enum

sealed abstract class LineItemNature(val name: String)

object LineItemNature extends Enum {
  sealed abstract class EnumVal(name: String) extends LineItemNature(name) with Value

  val Product = new EnumVal("Purchase"){}
  val Discount = new EnumVal("Discount"){}
  val Tax = new EnumVal("Tax"){}
  val Fee = new EnumVal("Fee"){}
  val Summary = new EnumVal("Summary") {}
  val Payment = new EnumVal("Payment"){}
  val Refund = new EnumVal("Refund") {}
}
