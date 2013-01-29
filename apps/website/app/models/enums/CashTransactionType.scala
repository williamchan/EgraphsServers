package models.enums

import egraphs.playutils.Enum

/**
 * The type of the CashTransaction. :-)
 */
object CashTransactionType extends Enum {
  sealed trait EnumVal extends Value

  val EgraphPurchase = new EnumVal {val name = "EgraphPurchase"}

  val PrintOrderPurchase = new EnumVal {val name = "PrintOrderPurchase"}

  val PurchaseRefund = new EnumVal {val name = "PurchaseRefund"}

  val CelebrityDisbursement = new EnumVal {val name = "CelebrityDisbursement"}

  val Checkout = new EnumVal {val name = "Checkout"}
}

trait HasCashTransactionType[T] {
  def _cashTransactionType: String

  def cashTransactionType: CashTransactionType.EnumVal = {
    CashTransactionType(_cashTransactionType).getOrElse(
      throw new IllegalArgumentException(_cashTransactionType)
    )
  }

  def withCashTransactionType(enum: CashTransactionType.EnumVal): T
}
