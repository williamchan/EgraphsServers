package models.enums

import egraphs.playutils.Enum

object AdminRole extends Enum {
  sealed trait EnumVal extends Value

  val Superuser = new EnumVal {
    val name = "Superuser"
  }
  val AdminDisabled = new EnumVal {
    val name = "AdminDisabled"
  }
}

trait HasAdminRole[T] {
  def _role: String

  def role: AdminRole.EnumVal = {
    AdminRole(_role).getOrElse(
      throw new IllegalArgumentException(_role)
    )
  }

  def withRole(status: AdminRole.EnumVal): T
}
