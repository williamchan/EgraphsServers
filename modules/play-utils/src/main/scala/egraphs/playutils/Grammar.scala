package egraphs.playutils

abstract class Grammar(
  val subjectPronoun: String,
  val objectPronoun: String,
  val possessiveAdjective: String,
  val possessivePronoun: String,
  val reflexivePronoun: String,
  val toHave: String,
  val toBe: String
) {

  def regularVerb(verb: String): String
}

object MaleGrammar extends Grammar(
  subjectPronoun = "he",
  objectPronoun = "him",
  possessiveAdjective = "his",
  possessivePronoun = "his",
  reflexivePronoun = "himself",
  toHave = "has",
  toBe = "is"
) {

  def regularVerb(verb: String): String = {
    verb + "s"
  }
}

object FemaleGrammar extends Grammar(
  subjectPronoun = "she",
  objectPronoun = "her",
  possessiveAdjective = "her",
  possessivePronoun = "hers",
  reflexivePronoun = "herself",
  toHave = "has",
  toBe = "is"
) {

  def regularVerb(verb: String): String = {
    verb + "s"
  }
}

object NeutralGrammar extends Grammar(
  subjectPronoun = "they",
  objectPronoun = "them",
  possessiveAdjective = "their",
  possessivePronoun = "theirs",
  reflexivePronoun = "themselves",
  toHave = "have",
  toBe = "are"
 ) {

  def regularVerb(verb: String): String = {
    verb
  }
}

object GrammarUtils {
  def egraphOrEgraphs(numberOfEgraphs: Int): String = {
    if (numberOfEgraphs == 1) "Egraph"
    else "Egraphs"
  }

  def getGrammarByGender(gender: Gender.EnumVal): Grammar = {
    gender match {
      case Gender.Neutral => NeutralGrammar
      case Gender.Male => MaleGrammar
      case _ => FemaleGrammar
    }
  }
}