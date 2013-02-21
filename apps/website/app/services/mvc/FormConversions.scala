package services.mvc

import services.http.forms.FormField
import services.http.forms.FormError
import services.http.forms.purchase.{CheckoutShippingForm, CheckoutBillingForm, PersonalizeForm}
import models.frontend.storefront.{PersonalizeForm => PersonalizeFormView, CheckoutShippingAddressFormView, CheckoutBillingInfoView, CheckoutFormView, PersonalizeMessageOption}
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

    /**
     *  Turns a FormField into a view field given that you provide a small conversion.
     *
     *  Usage:
     *  {{{
     *    import FormConversions._
     *
     *    val modelField:FormField[Money] = // This exists in some form somewhere
     *    val viewField:Field[String] = modelField.asViewFieldWithConversion { money =>
     *      money.getAmount().toString
     *    }
     *  }}}
     * @param convertToViewValue function that turns the model field's type into the view field's.
     * @tparam ViewFieldT type of the view field
     * @return a frontend Field[ViewFieldT]
     */
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

    /**
     * Converts the [[services.http.forms.purchase.PersonalizeForm]] into its front-end view analog.
     */
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
        noteToCelebrity = form.noteToCelebrity.asViewField,
        coupon = form.coupon.asViewField
      )
    }

    private def asPersonalizeMessageOption(request: WrittenMessageRequest) = {
      import WrittenMessageRequest.{CelebrityChoosesMessage, SignatureOnly, SpecificMessage}

      request match {
        case SpecificMessage => PersonalizeMessageOption.SpecificMessage
        case SignatureOnly => PersonalizeMessageOption.SignatureOnly
        case CelebrityChoosesMessage => PersonalizeMessageOption.CelebrityChoosesMessage
        case other => throw new IllegalStateException("models.enums.WrittenMessageRequest cannot have value = " + other)
      }
    }
  }

  class CheckoutBillingFormViewConversions(form: CheckoutBillingForm) {

    /**
     * Converts the [[services.http.forms.purchase.CheckoutBillingForm]] into its
     * front-end view analog.
     */
    def asCheckoutPageView: CheckoutBillingInfoView = {
      CheckoutBillingInfoView(
        fullName = form.name.asViewField,
        email = form.email.asViewField,
        postalCode = form.postalCode.asViewField
      )
    }
  }

  class CheckoutShippingFormViewConversions(form: CheckoutShippingForm) {

    /**
     * Converts the [[services.http.forms.purchase.CheckoutShippingForm]] into its
     * front-end view analog.
     */
    def asCheckoutPageView: CheckoutShippingAddressFormView = {
      CheckoutShippingAddressFormView(
        fullName = form.name.asViewField,
        address1 = form.address1.asViewField,
        address2 = form.address2.asViewField,
        city = form.city.asViewField,
        state = form.state.asViewField,
        postalCode = form.postalCode.asViewField,
        billingIsSameAsShipping = form.billingIsSameAsShipping.asViewFieldWithConversion(bool => bool)
      )
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

  implicit def checkoutBillingFormToViewConverter(form: CheckoutBillingForm)
  : CheckoutBillingFormViewConversions = {
    new CheckoutBillingFormViewConversions(form)
  }

  implicit def checkoutShippingFormToViewConverter(form: CheckoutShippingForm)
  : CheckoutShippingFormViewConversions = {
    new CheckoutShippingFormViewConversions(form)
  }

}
