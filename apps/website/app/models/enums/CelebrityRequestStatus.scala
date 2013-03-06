package models.enums

import services.Utils
import egraphs.playutils.Enum

object CelebrityRequestStatus extends Enum {
  sealed trait EnumVal extends Value

  val PendingAdminReview = new EnumVal {
    val name = "PendingAdminReview"
  }

  // if this request is already aliased and requires no admin action
  val Accepted = new EnumVal {
    val name = "Accepted"
  }

  val Rejected = new EnumVal {
    val name = "Rejected"
  }

  val Notified = new EnumVal {
    val name = "Notified"
  }
}

trait HasCelebrityRequestStatus[T] {
  def _celebrityRequestStatus: String

  def celebrityRequestStatus: CelebrityRequestStatus.EnumVal = {
    CelebrityRequestStatus(_celebrityRequestStatus).getOrElse(
      throw new IllegalArgumentException(_celebrityRequestStatus)
    )
  }

  def withCelebrityRequestStatus(status: CelebrityRequestStatus.EnumVal): T
}