package models.enums

import services.Utils

object RecipientChoice extends Utils.Enum {
  sealed abstract class EnumVal(val name: String) extends Value

  object Self extends EnumVal("Self")
  object Other extends EnumVal("Other")
}
