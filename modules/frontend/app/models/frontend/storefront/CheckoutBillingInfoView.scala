package models.frontend.storefront

import models.frontend.forms.Field

/**
 * View of the checkout screen's billing info form.
 * See [[views.frontend.html.celebrity_storefront_checkout]]
 *
 * @param fullName the buyer's name
 * @param email the buyer's email
 * @param postalCode the buyer's postal code
 */
case class CheckoutBillingInfoView(
  fullName: Field[String],
  email: Field[String],
  postalCode: Field[String]
)

object CheckoutBillingInfoView {
  /** Returns an empty version of the view, with parameter names as specified. */
  def empty(
    fullNameParam: String,
    emailParam: String,
    zipParam: String
  ): CheckoutBillingInfoView = {
    CheckoutBillingInfoView(
      Field.empty(fullNameParam),
      Field.empty(emailParam),
      Field.empty(zipParam)
    )
  }
}
