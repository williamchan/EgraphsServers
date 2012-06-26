package models.enums

import services.Utils

/**
 * Encodes the choice by the buyer of an egraph whether to request a specific
 * hand-written message, have the celebrity write his own, or have him write
 * his signature only.
 */
sealed abstract class WrittenMessageChoice(val name: String)

object WrittenMessageChoice extends Utils.Enum {
  sealed abstract class EnumVal(name: String) extends WrittenMessageChoice(name) with Value

  object SpecificMessage extends EnumVal("SignatureWithMessage")

  // Let the celebrity write whatever they want
  object CelebrityChoosesMessage extends EnumVal("CelebrityChoosesMessage")

  object SignatureOnly extends EnumVal("SignatureOnly")
}

trait HasWrittenMessageChoice[T] {
  def _orderType: String

  def writtenMessageChoice: WrittenMessageChoice = {
    WrittenMessageChoice(_orderType).getOrElse(
      throw new IllegalArgumentException(_orderType)
    )
  }

  def withWrittenMessageChoice(enum: WrittenMessageChoice): T
}
