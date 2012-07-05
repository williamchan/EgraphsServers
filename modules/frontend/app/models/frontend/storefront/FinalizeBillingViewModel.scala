package models.frontend.storefront

case class FinalizeBillingViewModel (
  name: String,
  email: String,
  postalCode: String,
  paymentToken: String,
  paymentApiKey: String,
  paymentJsModule: String,
  editUrl: String
)