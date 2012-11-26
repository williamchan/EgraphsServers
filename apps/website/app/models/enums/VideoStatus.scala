package models.enums

import services.Utils
import egraphs.playutils.Enum

object VideoStatus extends Enum {
  sealed trait EnumVal extends Value

  val Unprocessed = new EnumVal {
    val name = "Unprocessed"
  }
  val Approved = new EnumVal {
    val name = "Approved"
  }
  val Rejected = new EnumVal {
    val name = "Rejected"
  }
}

trait HasVideoStatus[T] {
  def _videoStatus: String

  def videoStatus: VideoStatus.EnumVal = {
    VideoStatus(_videoStatus).getOrElse(
      throw new IllegalArgumentException(_videoStatus)
    )
  }

  def withVideoStatus(status: VideoStatus.EnumVal): T
}