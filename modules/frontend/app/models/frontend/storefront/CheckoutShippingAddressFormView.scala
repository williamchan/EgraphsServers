package models.frontend.storefront

import models.frontend.forms.Field

case class CheckoutShippingAddressFormView(
  fullName: Field[String],
  phone: Field[String],
  email: Field[String],
  address1: Field[String],
  address2: Field[String],
  city: Field[String],
  state: Field[String],
  postalCode: Field[String]
)

object CheckoutShippingAddressFormView {
  def empty(
    fullNameParam: String,
    phoneParam: String,
    emailParam: String,
    address1Param: String,
    address2Param: String,
    cityParam: String,
    stateParam: String,
    postalCodeParam: String
    ): CheckoutShippingAddressFormView = {
    CheckoutShippingAddressFormView(
      Field.empty(fullNameParam),
      Field.empty(phoneParam),
      Field.empty(emailParam),
      Field.empty(address1Param),
      Field.empty(address2Param),
      Field.empty(cityParam),
      Field.empty(stateParam),
      Field.empty(postalCodeParam)
    )
  }
}