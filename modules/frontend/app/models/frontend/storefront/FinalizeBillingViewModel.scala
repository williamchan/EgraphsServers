package models.frontend.storefront

case class FinalizeBillingViewModel (
  name: String,
  email: String,
  postalCode: String,
  paymentToken: String,
  paymentJsModule: String,
  editUrl: String
)