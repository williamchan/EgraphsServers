package models.enums

import egraphs.playutils.Enum

/**
 * Enum for describing whether an the type of a bank account.
 */
object BankAccountType extends Enum {
  sealed trait EnumVal extends Value

  val Published = new EnumVal {
    val name = "Checking"
  }
  val Unpublished = new EnumVal {
    val name = "Savings"
  }
}

trait HasDepositAccountType[T] {
  def _depositAccountType: Option[String]

  def depositAccountType: Option[BankAccountType.EnumVal] = {
    _depositAccountType.map( depositAccountType => BankAccountType(depositAccountType).getOrElse(
      throw new IllegalArgumentException(depositAccountType)
    ))
  }

  def withDepositAccountType(depositAccountType: BankAccountType.EnumVal): T
}

// Note: make HasPaymentAccountType if we even need that, don't reuse HasDepositAccount for that.