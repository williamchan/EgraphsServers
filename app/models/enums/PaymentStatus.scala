package models.enums

import services.Utils

object PaymentStatus extends Utils.Enum {
  sealed trait EnumVal extends Value

  val NotCharged = new EnumVal {
    val name = "NotCharged"
  }
  val Charged = new EnumVal {
    val name = "Charged"
  }
  val Refunded = new EnumVal {
    val name = "Refunded"
  }
}

trait HasPaymentStatus[T] {
  def _paymentStatus: String

  def paymentStatus: PaymentStatus.EnumVal = {
    PaymentStatus(_paymentStatus).getOrElse(
      throw new IllegalArgumentException(_paymentStatus)
    )
  }

  def withPaymentStatus(status: PaymentStatus.EnumVal): T
}
