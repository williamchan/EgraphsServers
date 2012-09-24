package models.frontend.storefront

import models.frontend.forms.Field

/**
 * Shipping form as rendered in the CheckoutScreen
 *
 * See [[views.frontend.html.celebrity_storefront_checkout]]
 *
 * @param fullName name of the shipping recipient
 * @param address1 first address line of the shipping recipient
 * @param address2 second address line of the shipping recipient
 * @param city city for shipping
 * @param state state for shipping
 * @param postalCode postal (ZIP) code for shipping
 * @param billingIsSameAsShipping true that we should use the information here
 *   for billing purposes as well.
 */
case class CheckoutShippingAddressFormView(
  fullName: Field[String],
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
    address1Param: String,
    address2Param: String,
    cityParam: String,
    stateParam: String,
    postalCodeParam: String,
    billingIsSameAsShippingParam: String
    ): CheckoutShippingAddressFormView = {
    CheckoutShippingAddressFormView(
      Field.empty(fullNameParam),
      Field.empty(address1Param),
      Field.empty(address2Param),
      Field.empty(cityParam),
      Field.empty(stateParam),
      Field.empty(postalCodeParam),
      Field(billingIsSameAsShippingParam, Some(true))
    )
  }
}