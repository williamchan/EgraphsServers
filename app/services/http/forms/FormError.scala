package services.http.forms

trait FormError {
  def description: String
}

class SimpleFormError(val description: String) extends FormError

case class ValueNotPresentFieldError(description: String = "Required") extends FormError

class DependentFieldError extends SimpleFormError("Dependent field was invalid")

