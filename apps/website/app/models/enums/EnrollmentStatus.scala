package models.enums

import services.Utils

object EnrollmentStatus extends Utils.Enum {
  sealed trait EnumVal extends Value

  val NotEnrolled = new EnumVal {
    val name = "NotEnrolled"
  }
  val AttemptingEnrollment = new EnumVal {
    val name = "AttemptingEnrollment"
  }
  val Enrolled = new EnumVal {
    val name = "Enrolled"
  }
  val FailedEnrollment = new EnumVal {
    val name = "FailedEnrollment"
  }
}

trait HasEnrollmentStatus[T] {
  def _enrollmentStatus: String

  def enrollmentStatus: EnrollmentStatus.EnumVal = {
    EnrollmentStatus(_enrollmentStatus).getOrElse(
      throw new IllegalArgumentException(_enrollmentStatus)
    )
  }

  def withEnrollmentStatus(status: EnrollmentStatus.EnumVal): T
}
