package services.http.forms.purchase

import services.http.forms.{FormField, DependentFieldError, FormChecks, Form}

/**
 * Purchase flow form for billing info
 */
class CheckoutBillingForm(
  val paramsMap: Form.Readable,
  check: FormChecks,
  checkPurchaseField: PurchaseFormChecksFactory,
  shippingFormOption: Option[CheckoutShippingForm]
) extends Form[CheckoutBillingForm.Valid] {

  import CheckoutBillingForm.Params

  val paymentToken = field(Params.PaymentToken).validatedBy { paramValues =>
    checkPurchaseField(paramValues).isPaymentToken
  }

  val name = field(Params.Name).validatedBy { paramValues =>
    delegateToShippingForm(shipping => shipping.name).getOrElse {
      checkPurchaseField(paramValues).isName
    }
  }

  val email = field(Params.Email).validatedBy { paramValues =>
    delegateToShippingForm(shipping => shipping.email).getOrElse {
      checkPurchaseField(paramValues).isEmail
    }
  }

  val postalCode = field(Params.PostalCode).validatedBy { paramValues =>
    delegateToShippingForm(shipping => shipping.postalCode).getOrElse {
      checkPurchaseField(paramValues).isZipCode
    }
  }

  protected def formAssumingValid: CheckoutBillingForm.Valid = {
    CheckoutBillingForm.Valid(
      paymentToken.value.get,
      name.value.get,
      email.value.get,
      postalCode.value.get
    )
  }

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
    val ShippingSameAsBilling = "order.shipping.sameAsBilling"
  }

  case class Valid(paymentToken: String, name: String, email: String, postalCode: String)
}