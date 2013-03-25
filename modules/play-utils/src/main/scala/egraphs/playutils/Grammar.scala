package egraphs.playutils

object Grammar {

  def subjectPronoun(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    genderAppropriateWord(gender, capitalize, "he", "she", "they")
  }

  def objectPronoun(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    genderAppropriateWord(gender, capitalize, "him", "her", "them")
  }

  def possessivePronoun(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    genderAppropriateWord(gender, capitalize, "his", "her", "their")
  }
  
  def irregularToHave(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    genderAppropriateWord(gender, capitalize, "has", "has", "have")
  }

  def irregularToBe(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    genderAppropriateWord(gender, capitalize, "is", "is", "are")
  }

  def regularVerb(verb: String, gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    genderAppropriateWord(gender, capitalize, verb + "s", verb + "s", verb)
  }

  def egraphOrEgraphs(numberOfEgraphs: Int): String = {
    if (numberOfEgraphs == 1) "Egraph"
    else "Egraphs"
  }

  private def genderAppropriateWord(gender: Gender.EnumVal, capitalize: Boolean,
    maleWord: String, femaleWord: String, neutralWord: String): String = {

    val lowercaseWord = gender match {
      case (Gender.Male) => maleWord
      case (Gender.Female) => femaleWord
      case (Gender.Neutral) => neutralWord
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
    capitalizeOrNo(lowercaseWord, capitalize)
  }

  private def capitalizeOrNo(word: String, capitalize: Boolean): String = {
    if (capitalize) word.capitalize
    else word
  }
}