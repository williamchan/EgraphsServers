package models.frontend.storefront

import models.frontend.forms.Field

case class CheckoutBillingInfoView(
  fullName: Field[String],
  email: Field[String],
  postalCode: Field[String]
)

object CheckoutBillingInfoView {
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
