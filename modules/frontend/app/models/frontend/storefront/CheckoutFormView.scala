package models.frontend.storefront

import models.frontend.forms.Field

case class CheckoutFormView(
  actionUrl: String,
  billing: CheckoutBillingInfoView,
  shipping: Option[CheckoutShippingAddressFormView]
)


object CheckoutFormView {
  def empty(
    actionUrl: String,
    billing: CheckoutBillingInfoView,
    shipping: Option[CheckoutShippingAddressFormView]
  ): CheckoutFormView = {
    CheckoutFormView(
      actionUrl = actionUrl,
      billing = billing,
      shipping = shipping
    )
  }
}

// TODO: add shipping method