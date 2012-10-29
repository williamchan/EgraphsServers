package models.enums

import services.Utils

object OrderReviewStatus extends Utils.Enum {
  sealed trait EnumVal extends Value

  val PendingAdminReview = new EnumVal {
    val name = "PendingAdminReview"
  }
  val ApprovedByAdmin = new EnumVal {
    val name = "ApprovedByAdmin"
  }
  val RejectedByAdmin = new EnumVal {
    val name = "RejectedByAdmin"
  }
  val RejectedByCelebrity = new EnumVal {
    val name = "RejectedByCelebrity"
  }
}

trait HasOrderReviewStatus[T] {
  def _reviewStatus: String

  def reviewStatus: OrderReviewStatus.EnumVal = {
    OrderReviewStatus(_reviewStatus).getOrElse(
      throw new IllegalArgumentException(_reviewStatus)
    )
  }

  def withReviewStatus(status: OrderReviewStatus.EnumVal): T
}
