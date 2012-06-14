package services.http.forms

trait FormError {
  def description: String
}

class SimpleFormError(val description: String) extends FormError

case class ValueNotPresentFieldError(description: String = "Required") extends FormError

case object DependentFieldError extends SimpleFormError("")  // These ones don't produce an error in the front end
