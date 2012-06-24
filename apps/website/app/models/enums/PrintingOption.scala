package models.enums

import services.Utils

object PrintingOption extends Utils.Enum {
  sealed trait EnumVal extends Value

  val HighQualityPrint = new EnumVal {
    val name = "HighQualityPrint"
  }

  val DoNotPrint = new EnumVal {
    val name = "DoNotPrint"
  }
}
