package models.enums

import services.Utils

object OrderType extends Utils.Enum {
  sealed trait EnumVal extends Value

  val SignatureWithMessage = new EnumVal {
    val name = "SignatureWithMessage"
  }
  val SignatureOnly = new EnumVal {
    val name = "SignatureOnly"
  }
}

trait HasOrderType[T] {
  def _orderType: String

  def orderType: OrderType.EnumVal = {
    OrderType(_orderType).getOrElse(
      throw new IllegalArgumentException(_orderType)
    )
  }

  def withOrderType(enum: OrderType.EnumVal): T
}
