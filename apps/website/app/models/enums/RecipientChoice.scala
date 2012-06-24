package models.enums

import services.Utils

object RecipientChoice extends Utils.Enum {
  sealed trait EnumVal extends Value

  val Self = new EnumVal {
    val name = "Self"
  }

  // Let the celebrity write whatever they want
  val Other = new EnumVal {
    val name = "Other"
  }
}
