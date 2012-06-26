package models.enums

import services.Utils

/**
 * Encodes the choice by the buyer of an egraph whether to request a specific
 * hand-written message, have the celebrity write his own, or have him write
 * his signature only.
 */
sealed abstract class WrittenMessageRequest(val name: String)

object WrittenMessageRequest extends Utils.Enum {
  sealed abstract class EnumVal(name: String) extends WrittenMessageRequest(name) with Value

  object SpecificMessage extends EnumVal("SignatureWithMessage")

  // Let the celebrity write whatever they want
  object CelebrityChoosesMessage extends EnumVal("CelebrityChoosesMessage")

  object SignatureOnly extends EnumVal("SignatureOnly")
}

trait HasWrittenMessageRequest[T] {
  def _writtenMessageRequest: String

  def writtenMessageRequest: WrittenMessageRequest = {
    WrittenMessageRequest(_writtenMessageRequest).getOrElse(
      throw new IllegalArgumentException(_writtenMessageRequest)
    )
  }

  def withWrittenMessageRequest(enum: WrittenMessageRequest): T
}
