package services.http.forms.purchase

import services.http.forms.{FormChecks, Form}
import services.http.forms.purchase.ShippingInfoForm.Valid

/**
 * Purchase flow form for shipping information.
 */
class ShippingInfoForm(
  val paramsMap: Form.Readable,
  check: FormChecks,
  checkPurchaseField: PurchaseFormChecksFactory
) extends Form[ShippingInfoForm.Valid]
{
  import ShippingInfoForm.Params

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
    Right(paramValues.headOption)
  }

  val city = field(Params.City).validatedBy { paramValues =>
    check.isSomeValue(paramValues)
  }

  val postalCode = field(Params.PostalCode).validatedBy { paramValues =>
    for (
      paramValue <- check.isSomeValue(paramValues).right;
      _ <- check.isTrue(paramValue.length == 5, "Invalid postal code").right;
      _ <- check.isInt(paramValue, "Invalid postal code").right
    ) yield {
      paramValue
    }
  }

  //
  // Form members
  //
  protected def formAssumingValid: Valid = {
    ShippingInfoForm.Valid(
      name.value.get,
      address1.value.get,
      address2.value.get,
      city.value.get,
      postalCode.value.get
    )
  }
}

object ShippingInfoForm {
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
    postalCode: String
  )
}
