package models.enums

import services.Utils

sealed abstract class PrintingOption(val name: String)

object PrintingOption extends Utils.Enum {
  sealed abstract class EnumVal(name: String) extends PrintingOption(name) with Value

  object HighQualityPrint extends EnumVal("HighQualityPrint")
  object DoNotPrint extends EnumVal("DoNotPrint")
}
