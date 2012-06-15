package services.mvc

import services.http.forms.FormField
import services.http.forms.FormError

/**
 * Converts back-end models of forms into front-end models
 */
object FormConversions {
  import models.frontend.{forms => formsview}

  class FormFieldModelViewConversions(modelField: FormField[_]) {
    def asViewField(withErrors: Boolean): formsview.Field[String] = {
      formsview.Field[String](
        name=modelField.name,
        values=modelField.stringsToValidate.headOption,
        error=if (withErrors) modelField.error.map(error => error.asViewError) else None
      )
    }
  }

  class FormErrorModelViewConversions(modelError: FormError) {
    def asViewError: formsview.FormError = {
      formsview.FormError(modelError.description)
    }
  }

  implicit def modelFieldToView(modelField: FormField[_]): FormFieldModelViewConversions = {
    new FormFieldModelViewConversions(modelField)
  }

  implicit def modelFormErrorToView(modelError: FormError): FormErrorModelViewConversions = {
    new FormErrorModelViewConversions(modelError)
  }
}
