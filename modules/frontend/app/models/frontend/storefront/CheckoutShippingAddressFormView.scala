package models.frontend.storefront

import models.frontend.forms.Field

case class CheckoutShippingAddressFormView(
  fullName: Field[String],
  email: Field[String],
  address1: Field[String],
  address2: Field[String],
  city: Field[String],
  state: Field[String],
  postalCode: Field[String],
  billingIsSameAsShipping: Field[Boolean]
)

object CheckoutShippingAddressFormView {
  def empty(
    fullNameParam: String,
    emailParam: String,
    address1Param: String,
    address2Param: String,
    cityParam: String,
    stateParam: String,
    postalCodeParam: String,
    billingIsSameAsShippingParam: String
    ): CheckoutShippingAddressFormView = {
    CheckoutShippingAddressFormView(
      Field.empty(fullNameParam),
      Field.empty(emailParam),
      Field.empty(address1Param),
      Field.empty(address2Param),
      Field.empty(cityParam),
      Field.empty(stateParam),
      Field.empty(postalCodeParam),
      Field(billingIsSameAsShippingParam, Some(true))
    )
  }
}