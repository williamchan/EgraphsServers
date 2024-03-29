package egraphs.playutils

abstract sealed class Grammar(
  val subjectPronoun: String,
  val objectPronoun: String,
  val possessiveAdjective: String,
  val possessivePronoun: String,
  val reflexivePronoun: String,
  val toHave: String,
  val toBe: String
) {

  def regularVerb(verb: String): String
  def stemChangingVerb(verb: String): String // y to ie
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

  def stemChangingVerb(verb: String): String = {
    verb.dropRight(1) + "ies"
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

  def stemChangingVerb(verb: String): String = {
    verb.dropRight(1) + "ies"
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

  def stemChangingVerb(verb: String): String = {
    regularVerb(verb)
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