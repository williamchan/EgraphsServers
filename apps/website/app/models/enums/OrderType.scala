package models.enums

import egraphs.playutils.Enum

object OrderType extends Enum {
  sealed trait EnumVal extends Value
  
  /** Includes paid egraphs and non-promotional freegraphs */
  val Normal = new EnumVal {
    val name = "Normal"
  }
  
  val Promotional = new EnumVal {
    val name = "Promotional"
  }
}

trait HasOrderType[T] {
  def _orderType: String
  
  def orderType: OrderType.EnumVal = {
    OrderType(_orderType).getOrElse(
      throw new IllegalArgumentException(_orderType)
    )
  }
  
  def withOrderType(orderType: OrderType.EnumVal): T
}