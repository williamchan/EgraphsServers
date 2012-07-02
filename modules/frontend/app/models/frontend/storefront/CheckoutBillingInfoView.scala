package models.frontend.storefront

import models.frontend.forms.Field

case class CheckoutBillingInfoView(
  fullName: Field[String],
  email: Field[String],
  postalCode: Field[String],
  paymentToken: Field[String],
  cardNumber: Field[String],
  cardExpiryMonth: Field[String],
  cardExpiryYear: Field[String],
  cardCvv: Field[String]
)

object CheckoutBillingInfoView {
  def empty(
    fullNameParam: String,
    emailParam: String,
    zipParam: String,
    paymentTokenParam: String,
    cardNumberParam: String,
    cardExpiryMonthParam: String,
    cardExpiryYearParam: String,
    cardCvvParam: String
  ): CheckoutBillingInfoView = {
    CheckoutBillingInfoView(
      Field.empty(fullNameParam),
      Field.empty(emailParam),
      Field.empty(zipParam),
      Field.empty(paymentTokenParam),
      Field.empty(cardNumberParam),
      Field.empty(cardExpiryMonthParam),
      Field.empty(cardExpiryYearParam),
      Field.empty(cardCvvParam)
    )
  }
}
