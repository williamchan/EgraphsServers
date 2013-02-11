package models.checkout.data

import play.api.data.{Forms, Form, FormError}
import play.api.mvc.Request

/**
 * Mixin for mapping form errors to the api's specified error format.
 *
 * TODO(CE-13): use custom Form Contraints so that the error content is valid, just need to rearrange the json tree to
 * the api's spec.
 */
trait CheckoutForm[T] {
  /** binds form with api errors instead of normal errors */
  def bindFromRequest()(implicit request: Request[_]): Form[T] = {
    val boundForm = form.bindFromRequest()
    transformToApiErrors(boundForm.errors) match {
      case Nil => boundForm
      case apiErrors => boundForm.copy(errors = apiErrors, value = None)
    }
  }

  /**
   * This maps form errors to the errors expected by users of the checkout api.
   *
   * @param formErrors from binding form from a request (this assumes no semantic validation is included, just formatting)
   * @param additionalErrors additional errors may be added (this is for higher level validation, e.g. product inventory, etc)
   * @return Sequence of form errors matching api specs
   */
  def transformToApiErrors(formErrors: Seq[FormError], additionalErrors: Seq[(String, String)] = Nil): Seq[FormError] = {
    val fields = formErrors map (_.key)
    val fieldsAndErrors = additionalErrors ++ {
      for (field <- fields; error <- formErrorByField.get(field)) yield ( field -> error.name )
    }

    for ((field, error) <- fieldsAndErrors) yield FormError(field, error)
  }

  def form: Form[T]

  protected def formErrorByField: Map[String, ApiFormError]

}
