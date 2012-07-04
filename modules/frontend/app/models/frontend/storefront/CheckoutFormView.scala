package models.frontend.storefront

import models.frontend.forms.Field

/**
 * View of the checkout screen's form. It is actually composed of
 * several different forms.
 *
 * See [[views.frontend.html.celebrity_storefront_checkout]]
 *
 * @param actionUrl server target for the form
 * @param billing billing form
 * @param shipping optional shipping form (it's only necessary if the
 *   user previously specified that they wanted a physical egraph
 *   shipped to them.
 */
case class CheckoutFormView(
  actionUrl: String,
  billing: CheckoutBillingInfoView,
  shipping: Option[CheckoutShippingAddressFormView]
)

object CheckoutFormView {

  /**
   * A basic CheckoutFormView with empty values and
   * field names as provided in the arguments.
   */
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
