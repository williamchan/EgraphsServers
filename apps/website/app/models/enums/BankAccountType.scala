package models.enums

import play.api.libs.json._
import egraphs.playutils.Enum

/**
 * Enum for describing whether an the type of a bank account.
 */
object BankAccountType extends Enum {
  sealed trait EnumVal extends Value

  val Checking = new EnumVal {
    val name = "Checking"
  }
  val Savings = new EnumVal {
    val name = "Savings"
  }

  /**
   * This will let you more easily write BankAccountType to your json objects
   * using the api formatting.
   */
  implicit object ApiDateFormat extends Format[EnumVal] {
    def writes(accountType: EnumVal): JsValue = {
      JsString(accountType.name)
    }

    def reads(json: JsValue): JsResult[EnumVal] = {
      JsSuccess {
        BankAccountType(json.as[String]).get
      }
    }
  }
}

trait HasDepositAccountType[T] {
  def _depositAccountType: Option[String]

  def depositAccountType: Option[BankAccountType.EnumVal] = {
    _depositAccountType.map( depositAccountType => BankAccountType(depositAccountType).getOrElse(
      throw new IllegalArgumentException(depositAccountType)
    ))
  }

  def withDepositAccountType(maybeDepositAccountType: Option[BankAccountType.EnumVal]): T
}

// Note: make HasPaymentAccountType if we even need that, don't reuse HasDepositAccount for that.