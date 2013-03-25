package models.frontend

abstract class PersonalPronouns (
  val subject: String,
  val obj: String,
  val possessiveAdjective: String,
  val possessivePronoun: String,
  val reflexive: String
)

object MalePersonalPronouns extends PersonalPronouns (
  subject = "he",
  obj = "him",
  possessiveAdjective = "his",
  possessivePronoun = "his",
  reflexive = "himself"
)


object FemalePersonalPronouns extends PersonalPronouns (
  subject = "she",
  obj = "her",
  possessiveAdjective = "her",
  possessivePronoun = "hers",
  reflexive = "herself"
)

object NeutralPersonalPronouns extends PersonalPronouns (
  subject = "they",
  obj = "them",
  possessiveAdjective = "their",
  possessivePronoun = "theirs",
  reflexive = "themselves"
 )