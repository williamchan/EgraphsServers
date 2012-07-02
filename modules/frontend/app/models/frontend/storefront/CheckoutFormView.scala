package models.frontend.storefront

import models.frontend.forms.Field

case class CheckoutFormView(
  actionUrl: String,
  billing: CheckoutBillingInfoView,
  shipping: Option[CheckoutShippingAddressFormView],
  shippingIsSameAsBilling: Field[Boolean]
)


object CheckoutFormView {
  def empty(
    actionUrl: String,
    billing: CheckoutBillingInfoView,
    shipping: Option[CheckoutShippingAddressFormView],
    shippingIsSameAsBillingParam: String
  ): CheckoutFormView = {
    CheckoutFormView(
      actionUrl = actionUrl,
      billing = billing,
      shipping = shipping,
      shippingIsSameAsBilling = Field(shippingIsSameAsBillingParam, shipping.map(_ => true))
    )
  }
}

// TODO: add shipping method