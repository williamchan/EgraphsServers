package models.frontend.storefront

case class FinalizeShippingViewModel (
  name: String,
  email: String,
  addressLine1: String,
  addressLine2: Option[String],
  city: String,
  state: String,
  postalCode: String,
  editUrl: String
)

