package models.enums

import egraphs.playutils.Enum

/**
 * Represents a printing option for an egraph order
 */
sealed abstract class PrintingOption(val name: String)

object PrintingOption extends Enum {
  sealed abstract class EnumVal(name: String) extends PrintingOption(name) with Value

  val HighQualityPrint = new EnumVal("HighQualityPrint") {}
  val DoNotPrint = new EnumVal("DoNotPrint") {}
}
