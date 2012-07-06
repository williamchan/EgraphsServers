package models.enums

import services.Utils

/**
 * Represents a recipient choice (either self or other) for an Egraph order.
 * Egraphs with a type of Other are gifts.
 */
sealed abstract class RecipientChoice(val name: String)

object RecipientChoice extends Utils.Enum {
  sealed abstract class EnumVal(name: String) extends RecipientChoice(name) with Value

  val Self = new EnumVal("Self") {}
  val Other = new EnumVal("Other") {}
}