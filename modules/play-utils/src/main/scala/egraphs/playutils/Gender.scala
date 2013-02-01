package egraphs.playutils

object Gender extends Enum {
  sealed trait EnumVal extends Value  

  val Male = new EnumVal {
    val name = "Male"
  }
  val Female = new EnumVal {
    val name = "Female"
  }
  val Neutral = new EnumVal {
    val name = "Neutral"
  }  
}

trait HasGender[T] {
  def _gender: String

  def gender: Gender.EnumVal = {
    Gender(_gender).getOrElse(
      throw new IllegalArgumentException(_gender)
    )
  }

  def withGender(gender: Gender.EnumVal): T
}