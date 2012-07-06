package models.frontend.storefront

/**
 * The aggregate ViewModel used to render the finalize page.
 *
 * See [[views.frontend.html.celebrity_storefront_finalize]]
 *
 * @param billing the billing summary viewmodel
 * @param shipping the shipping summary viewmodel
 * @param personalization the personalization summary viewmodel
 * @param price the pricing summary viewmodel
 * @param purchaseUrl url to which the user's browser should POST in order
 *   to process the payment and finalize the order.
 */
case class FinalizeViewModel (
  billing: FinalizeBillingViewModel,
  shipping: Option[FinalizeShippingViewModel],
  personalization: FinalizePersonalizationViewModel,
  price: FinalizePriceViewModel,
  purchaseUrl: String
)