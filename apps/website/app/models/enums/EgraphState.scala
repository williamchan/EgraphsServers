package models.enums

import egraphs.playutils.Enum

object EgraphState extends Enum {
  sealed trait EnumVal extends Value

  val AwaitingVerification = new EnumVal {
    val name = "AwaitingVerification"
  }
  val Published = new EnumVal {
    val name = "Published"
  }
  val PassedBiometrics = new EnumVal {
    val name = "PassedBiometrics"
  }
  val FailedBiometrics = new EnumVal {
    val name = "FailedBiometrics"
  }
  val ApprovedByAdmin = new EnumVal {
    val name = "ApprovedByAdmin"
  }
  val RejectedByAdmin = new EnumVal {
    val name = "RejectedByAdmin"
  }

  /* The following states are required for MLB egraphs per agreement with the MLB Players Association. */
  val PendingMlbReview = new EnumVal {
    val name = "PendingMlbReview"
  }
  val RejectedByMlb = new EnumVal {
    val name = "RejectedByMlb"
  }
}

trait HasEgraphState[T] {
  def _egraphState: String

  def egraphState: EgraphState.EnumVal = {
    EgraphState(_egraphState).getOrElse(
      throw new IllegalArgumentException(_egraphState)
    )
  }

  def withEgraphState(enum: EgraphState.EnumVal): T
}
