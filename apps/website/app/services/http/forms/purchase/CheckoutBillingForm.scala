package services.http.forms.purchase

import services.http.forms.Form

/**
 * Purchase flow form for billing info
 */
class CheckoutBillingForm(
  val paramsMap: Form.Readable,
  check: PurchaseFormChecksFactory,
  shippingFormOption: Option[ShippingInfoForm]
) extends Form[CheckoutBillingForm.Valid] {

  import CheckoutBillingForm.Params

  val paymentToken = field(Params.PaymentToken).validatedBy { paramValues =>
    check(paramValues).isPaymentToken
  }

  val name = field(Params.Name).validatedBy { paramValues =>
    check(paramValues).isName
  }

  val email = field(Params.Email).validatedBy { paramValues =>
    check(paramValues).isEmail
  }

  val postalCode = field(Params.BillingPostalCode).validatedBy { paramValues =>
    check(paramValues).isZipCode
  }

  protected def formAssumingValid: CheckoutBillingForm.Valid = {
    CheckoutBillingForm.Valid(
      paymentToken.value.get,
      name.value.get,
      email.value.get,
      postalCode.value.get
    )
  }
}

object CheckoutBillingForm {
  object Params {
    val PaymentToken = "order.billing.token"
    val Name = "order.billing.name"
    val Email = "order.billing.email"
    val BillingPostalCode = "order.billing.postalCode"
    val ShippingSameAsBilling = "order.shipping.sameAsBilling"
  }

  case class Valid(paymentToken: String, name: String, email: String, postalCode: String)
}