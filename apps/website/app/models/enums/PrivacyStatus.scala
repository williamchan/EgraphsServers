package models.enums

import egraphs.playutils.Enum

object PrivacyStatus extends Enum {
  sealed trait EnumVal extends Value

  val Private = new EnumVal {
    val name = "Private"
  }
  val Public = new EnumVal {
    val name = "Public"
  }
}

trait HasPrivacyStatus[T] {
  def _privacyStatus: String

  def privacyStatus: PrivacyStatus.EnumVal = {
    PrivacyStatus(_privacyStatus).getOrElse(
      throw new IllegalArgumentException(_privacyStatus)
    )
  }

  def withPrivacyStatus(status: PrivacyStatus.EnumVal): T
}
