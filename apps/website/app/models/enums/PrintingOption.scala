package models.enums

import services.Utils


/**
 * Represents a printing option for an egraph order
 */
sealed abstract class PrintingOption(val name: String)

object PrintingOption extends Utils.Enum {
  sealed abstract class EnumVal(name: String) extends PrintingOption(name) with Value

  val HighQualityPrint = new EnumVal("HighQualityPrint") {}
  val DoNotPrint = new EnumVal("DoNotPrint") {}
}
