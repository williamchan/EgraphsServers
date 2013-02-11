package models.checkout.data

import play.api.data.Forms

object ApiForms {
  import play.api.data.validation._

  def text(min: Int = 0, max: Int = Int.MaxValue) = Forms.text verifying lengthRequirement(min, max)



  def lengthRequirement(min: Int = 0, max: Int = Int.MaxValue) = Constraint[String](ApiFormError.InvalidLength.name) {
    str => if (min <= str.length && str.length <= max) Valid else Invalid(ApiFormError.InvalidLength.name, str.length)
  }

  def formatRequirement(format: String) = Constraint[String](ApiFormError.InvalidFormat.name) {
    str => if (str.matches(format)) Valid else Invalid(ApiFormError.InvalidFormat.name, str)
  }

  def typeRequirement[T](conv: (String) => T) = Constraint[String](ApiFormError.InvalidType.name) {
    str => try { conv(str); Valid} catch { case _ => Invalid(ApiFormError.InvalidType.name, str)}
  }
}
