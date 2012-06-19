package services.http.forms

/** Abstraction of an error in a form */
trait FormError {
  def description: String
}

/**
 * Simplest form error type. Use this when you don't need to do any branching
 * on the error type.
 */
class SimpleFormError(val description: String) extends FormError {
  override def toString = {
    this.getClass.getSimpleName.toString + "(" + description + ")"
  }
}

/** Error caused by the field not having a value when one was required */
case class ValueNotPresentFieldError(description: String = "Required") extends FormError

/**
 * Error caused by the derived field being unable to calculate due to an error
 * in one of its dependent fields.
 */
class DependentFieldError extends SimpleFormError("Dependent field was invalid")

