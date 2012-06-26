package services.http.forms.purchase

import services.http.forms.Form


/**
 * Purchase flow form for billing info
 */
class BillingInfoForm(val paramsMap: Form.Readable, check: PurchaseFormChecksFactory)
  extends Form[BillingInfoForm.Valid]
{
  import BillingInfoForm.Params

  val paymentToken = field(Params.PaymentToken).validatedBy { paramValues =>
    check(paramValues).isPaymentToken
  }

  val name = field(Params.Name).validatedBy { paramValues =>
    check(paramValues).isName
  }

  val email = field(Params.Email).validatedBy { paramValues =>
    check(paramValues).isEmail
  }

  protected def formAssumingValid: BillingInfoForm.Valid = {
    BillingInfoForm.Valid(
      paymentToken.value.get,
      name.value.get,
      email.value.get
    )
  }
}

object BillingInfoForm {
  object Params {
    val PaymentToken = "order.billing.token"
    val Name = "order.billing.name"
    val Email = "order.billing.email"
  }

  case class Valid(paymentToken: String, name: String, email: String)
}