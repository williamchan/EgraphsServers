package models.enums

import egraphs.playutils.Enum

object CallToActionType extends Enum {
  sealed trait EnumVal extends Value

  val SimpleLink = new EnumVal{
    val name = "SimpleLink"
  }

  val InPageAction = new EnumVal {
    val name = "InPageAction"
  }

  val SearchBox = new EnumVal {
    val name = "Searchbox"
  }

  val VideoPlayer = new EnumVal {
    val name = "VideoPlayer"
  }
}

trait HasCallToActionType[T] {
  def _callToActionType: String
  def callToActionTarget: String

  def callToActionType: CallToActionType.EnumVal = {
    CallToActionType(_callToActionType).getOrElse(
      throw new IllegalArgumentException(_callToActionType)
    )
  }

  def withCallToActionType(value: CallToActionType.EnumVal) : T

}