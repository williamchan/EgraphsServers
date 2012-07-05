package models.frontend.storefront

case class FinalizeViewModel (
  billing: FinalizeBillingViewModel,
  shipping: Option[FinalizeShippingViewModel],
  personalization: FinalizePersonalizationViewModel,
  price: FinalizePriceViewModel,
  purchaseUrl: String
)