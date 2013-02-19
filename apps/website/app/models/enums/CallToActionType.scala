package models.enums

import egraphs.playutils.Enum
import models.frontend.masthead.{InPageActionViewModel, SearchBoxViewModel, VideoPlayerViewModel, SimpleLinkViewModel}

/**
 * Enum for Call To Action options on the homepage.
 *
 * Searchbox is the site search. Text represents the place holder text
 *
 * SimpleLink an egraphs orange arrow button that links to the URL in the target. In page anchor locations work also.
 *
 * VideoPlayer brings up the video modal with the education youtube video.
 */
object CallToActionType extends Enum {
  sealed trait EnumVal extends Value

  val SimpleLink = new EnumVal{
    val name = "SimpleLink"
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
      case SearchBox => SearchBoxViewModel(text, target)
      case VideoPlayer => VideoPlayerViewModel(text, target)
      case _ => VideoPlayerViewModel(text, target)
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