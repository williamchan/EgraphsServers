package models.enums

import services.Utils

object WrittenMessageChoice extends Utils.Enum {
  sealed trait EnumVal extends Value

  val SpecificMessage = new EnumVal {
    val name = "SignatureWithMessage"
  }

  // Let the celebrity write whatever they want
  val CelebrityChoosesMessage = new EnumVal {
    val name = "SignatureWithArbitraryMessage"
  }

  val SignatureOnly = new EnumVal {
    val name = "SignatureOnly"
  }
}

trait HasWrittenMessageChoice[T] {
  def _orderType: String

  def writtenMessageChoice: WrittenMessageChoice.EnumVal = {
    WrittenMessageChoice(_orderType).getOrElse(
      throw new IllegalArgumentException(_orderType)
    )
  }

  def withWrittenMessageChoice(enum: WrittenMessageChoice.EnumVal): T
}
