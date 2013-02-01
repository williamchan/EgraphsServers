package egraphs.playutils

object Grammar {

  def subjectPronoun(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "he"
      case (Gender.Male, true) => "He"
      case (Gender.Female, false) => "she"
      case (Gender.Female, true) => "She"
      case (Gender.Neutral, false) => "they"
      case (Gender.Neutral, true) => "They"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def objectPronoun(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "him"
      case (Gender.Male, true) => "Him"
      case (Gender.Female, false) => "her"
      case (Gender.Female, true) => "Her"
      case (Gender.Neutral, false) => "them"
      case (Gender.Neutral, true) => "Them"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def possessivePronoun(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "his"
      case (Gender.Male, true) => "His"
      case (Gender.Female, false) => "her"
      case (Gender.Female, true) => "Her"
      case (Gender.Neutral, false) => "their"
      case (Gender.Neutral, true) => "Their"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }
  
  def irregularToHave(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "has"
      case (Gender.Male, true) => "Has"
      case (Gender.Female, false) => "has"
      case (Gender.Female, true) => "Has"
      case (Gender.Neutral, false) => "have"
      case (Gender.Neutral, true) => "Have"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def regularVerb(verb: String, gender: Gender.EnumVal): String = {
    gender match {
      case Gender.Male => verb + "s"
      case Gender.Female => verb + "s"
      case Gender.Neutral => verb
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def egraphOrEgraphs(numberOfEgraphs: Int): String = {
    if (numberOfEgraphs == 1) "Egraph"
    else "Egraphs"
  }
}