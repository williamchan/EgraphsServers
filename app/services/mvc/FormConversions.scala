package services.mvc

import services.http.forms.FormField
import services.http.forms.FormError

/**
 * Converts back-end models of forms into front-end models
 * Usage:
 * {{{
 *   import FormConversions._
 *
 *   render_scala_tempalte(myFormField.asViewField)
 * }}}
 */
object FormConversions {
  import models.frontend.{forms => formsview}

  class FormFieldModelViewConversions(modelField: FormField[_]) {
    def asViewField: formsview.Field[String] = {
      formsview.Field[String](
        name=modelField.name,
        values=makeModelFieldStrings,
        error=modelField.error.map(error => error.asViewError)
      )
    }

    private def makeModelFieldStrings: Iterable[String] = {
      modelField.stringsToValidate match {
        case singular if singular.size <= 1 =>
          singular.headOption

        case plural =>
          plural
      }
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
