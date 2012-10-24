package models.enums

import services.Utils

object EgraphState extends Utils.Enum {
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
