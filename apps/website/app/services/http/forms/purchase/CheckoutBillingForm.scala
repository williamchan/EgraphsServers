package services.http.forms.purchase

import services.http.forms.{FormField, DependentFieldError, FormChecks, Form}

/**
 * Purchase flow form for billing info. It is a bit complex because the user can
 * specify that they want the settings from their [[services.http.forms.purchase.CheckoutShippingForm]]
 * to apply here where available.
 *
 * @param paramsMap the source from which to read the form
 * @param check low-level checks to use against the parameters read out of
 *   `paramsMap`
 * @param checkPurchaseField high-level checks against form variables specific
 *   to the purchase form.
 * @param shippingFormOption a shipping form, if one was provided. If this is,
 *   passed a real shipping form, the form validations here will delegate to the
 *   shipping form (and fail if the shipping form was invalid). This should
 *   _definitely_ be `None` if the customer chose not to order a print in the purchase
 *   flow.
 */
class CheckoutBillingForm(
  val paramsMap: Form.Readable,
  check: FormChecks,
  checkPurchaseField: PurchaseFormChecksFactory,
  shippingFormOption: Option[CheckoutShippingForm]
) extends Form[CheckoutBillingForm.Valid] {

  //
  // Field validations
  //
  import CheckoutBillingForm.Params

  /** Stripe token */
  val paymentToken = new OptionalField[String](Params.PaymentToken) {
    def validateIfPresent = {
      checkPurchaseField(List(stringToValidate)).isPaymentToken
    }
  }

  /** Name of the buyer, to be used for creating an account */
  val name = field(Params.Name).validatedBy { paramValues =>
    delegateToShippingForm(shipping => shipping.name).getOrElse {
      checkPurchaseField(paramValues).isName
    }
  }

  /** Buyer's email, to be used for keying the account we create */
  val email = field(Params.Email).validatedBy { paramValues =>
    checkPurchaseField(paramValues).isEmail
  }

  /** ZIP code */
  val postalCode = field(Params.PostalCode).validatedBy { paramValues =>
    delegateToShippingForm(shipping => shipping.postalCode).getOrElse {
      checkPurchaseField(paramValues).isZipCode
    }
  }

  //
  // Form[T] implementations
  //
  protected def formAssumingValid: CheckoutBillingForm.Valid = {
    CheckoutBillingForm.Valid(
      paymentToken.value.get,
      name.value.get,
      email.value.get,
      postalCode.value.get
    )
  }

  //
  // Private members
  //
  private def delegateToShippingForm[T](getShippingField: CheckoutShippingForm => FormField[T])
  : Option[Either[DependentFieldError, T]] = {
    shippingFormOption.map(shipping => check.dependentFieldIsValid(getShippingField(shipping)))
  }
}

object CheckoutBillingForm {
  object Params {
    val PaymentToken = "order.billing.token"
    val Name = "order.billing.name"
    val Email = "order.billing.email"
    val PostalCode = "order.billing.postalCode"
  }

  case class Valid(paymentToken: Option[String], name: String, email: String, postalCode: String)
}