package models.enums

import egraphs.playutils.Enum
import models.frontend.masthead.{InPageActionViewModel, SearchBoxViewModel, VideoPlayerViewModel, SimpleLinkViewModel}

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

  def toViewModel(callToActionType: CallToActionType.EnumVal, text: String, target: String) = {
    callToActionType match {
      case SimpleLink => SimpleLinkViewModel(text, target)
      case InPageAction => InPageActionViewModel(text,target)
      case SearchBox => SearchBoxViewModel(text, target)
      case VideoPlayer => VideoPlayerViewModel(text, target)
    }
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