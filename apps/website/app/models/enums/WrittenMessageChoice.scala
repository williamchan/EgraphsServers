package models.enums

import services.Utils

object WrittenMessageChoice extends Utils.Enum {
  sealed abstract class EnumVal(val name: String) extends Value

  object SpecificMessage extends EnumVal("SignatureWithMessage")

  // Let the celebrity write whatever they want
  object CelebrityChoosesMessage extends EnumVal("SignatureWithArbitraryMessage")

  object SignatureOnly extends EnumVal("SignatureOnly")
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
