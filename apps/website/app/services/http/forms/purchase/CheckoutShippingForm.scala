package services.http.forms.purchase

import services.http.forms.{FormChecks, Form}
import services.http.forms.purchase.CheckoutShippingForm.Valid

/**
 * Purchase flow form for shipping information.
 */
class CheckoutShippingForm(
  val paramsMap: Form.Readable,
  check: FormChecks,
  checkPurchaseField: PurchaseFormChecksFactory
) extends Form[CheckoutShippingForm.Valid]
{
  import CheckoutShippingForm.Params

  //
  // Field validations
  //
  val name = field(Params.Name).validatedBy { paramValues =>
    checkPurchaseField(paramValues).isName
  }

  val address1 = field(Params.AddressLine1).validatedBy { paramValues =>
    check.isSomeValue(paramValues.filter(value => value != ""))
  }

  val address2 = field(Params.AddressLine2).validatedBy { paramValues =>
    Right(paramValues.filter(value => value != "").headOption)
  }

  val city = field(Params.City).validatedBy { paramValues =>
    check.isSomeValue(paramValues)
  }

  val state = field(Params.State).validatedBy { paramValues =>
    check.isSomeValue(paramValues)
  }

  val postalCode = field(Params.PostalCode).validatedBy { paramValues =>
    checkPurchaseField(paramValues).isZipCode
  }

  //
  // Form members
  //
  protected def formAssumingValid: Valid = {
    CheckoutShippingForm.Valid(
      name.value.get,
      address1.value.get,
      address2.value.get,
      city.value.get,
      state.value.get,
      postalCode.value.get
    )
  }
}

object CheckoutShippingForm {
  object Params {
    val Name = "order.shipping.name"
    val AddressLine1 = "order.shipping.address1"
    val AddressLine2 = "order.shipping.address2"
    val City = "order.shipping.city"
    val State = "order.shipping.state"
    val PostalCode = "order.shipping.postalCode"
  }

  case class Valid(
    name: String,
    addressLine1: String,
    addressLine2: Option[String],
    city: String,
    state: String,
    postalCode: String
  )
}
