package models.checkout.data

import play.api.data.{Form, FormError}
import play.api.mvc.Request

trait CheckoutForm[T] {

  /** binds form with api errors instead of normal errors */
  def bindFromRequest()(implicit request: Request[_]): Form[T] = {
    val boundForm = form.bindFromRequest()
    transformToApiErrors(boundForm.errors) match {
      case Nil => boundForm
      case apiErrors => boundForm.copy(errors = apiErrors, value = None)
    }
  }

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
