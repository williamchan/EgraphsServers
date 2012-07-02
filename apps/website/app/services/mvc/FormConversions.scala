package services.mvc

import services.http.forms.FormField
import services.http.forms.FormError
import services.http.forms.purchase.PersonalizeForm
import models.frontend.storefront.{PersonalizeForm => PersonalizeFormView, PersonalizeMessageOption}
import models.frontend.forms.Field
import models.enums.{WrittenMessageRequest, RecipientChoice}

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

  class FormFieldModelViewConversions[+ModelFieldT](modelField: FormField[ModelFieldT]) {
    def asViewField: formsview.Field[String] = {
      formsview.Field[String](
        name=modelField.name,
        values=makeModelFieldStrings,
        error=modelField.independentError.map(error => error.asViewError)
      )
    }

    def asViewFieldWithConversion[ViewFieldT : Manifest](convertToViewValue: ModelFieldT => ViewFieldT)
    : Field[ViewFieldT] =
    {
      Field[ViewFieldT](
        modelField.name,
        modelField.value.map(modelValue => convertToViewValue(modelValue)),
        modelField.independentError.map(modelError => modelError.asViewError)
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

  class PersonalizeFormViewConversions(form: PersonalizeForm) {
    def asPersonalizeFormView(actionUrl: String): PersonalizeFormView = {
      val isGiftField = form.recipientChoice.asViewFieldWithConversion { recipientChoice =>
        (recipientChoice == RecipientChoice.Other)
      }

      val messageOptionField = form.writtenMessageRequest.asViewFieldWithConversion { msgRequest =>
        asPersonalizeMessageOption(msgRequest)
      }

      PersonalizeFormView(
        actionUrl = actionUrl,
        isGift = isGiftField,
        recipientName = form.recipientName.asViewField,
        recipientEmail = form.recipientEmail.asViewField,
        messageOption = messageOptionField,
        messageText = form.writtenMessageRequestText.asViewField,
        noteToCelebrity = form.noteToCelebrity.asViewField
      )
    }

    private def asPersonalizeMessageOption(request: WrittenMessageRequest) = {
      import WrittenMessageRequest.{CelebrityChoosesMessage, SignatureOnly, SpecificMessage}

      request match {
        case SpecificMessage => PersonalizeMessageOption.SpecificMessage
        case SignatureOnly => PersonalizeMessageOption.SignatureOnly
        case CelebrityChoosesMessage => PersonalizeMessageOption.AnythingHeWants
      }
    }
  }

  class FormErrorModelViewConversions(modelError: FormError) {
    def asViewError: formsview.FormError = {
      formsview.FormError(modelError.description)
    }
  }

  implicit def modelFieldToView[ModelValueT](modelField: FormField[ModelValueT])
  : FormFieldModelViewConversions[ModelValueT] =
  {
    new FormFieldModelViewConversions(modelField)
  }

  implicit def modelFormErrorToView(modelError: FormError): FormErrorModelViewConversions = {
    new FormErrorModelViewConversions(modelError)
  }

  implicit def personalizeFormToView(form: PersonalizeForm): PersonalizeFormViewConversions = {
    new PersonalizeFormViewConversions(form)
  }
}
