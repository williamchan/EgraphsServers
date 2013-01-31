package models.enums

import egraphs.playutils.Enum

/**
 * Encodes the choice by the buyer of an egraph whether to request a specific
 * hand-written message, have the celebrity write their own, or have them write
 * their signature only.
 */
sealed abstract class WrittenMessageRequest(val name: String)

object WrittenMessageRequest extends Enum {
  sealed abstract class EnumVal(name: String) extends WrittenMessageRequest(name) with Value

  val SpecificMessage = new EnumVal("SignatureWithMessage") {}

  // Let the celebrity write whatever they want
  val CelebrityChoosesMessage = new EnumVal("CelebrityChoosesMessage") {}

  val SignatureOnly = new EnumVal("SignatureOnly") {}
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
