package models.frontend.storefront

/**
 * Billing info ViewModel as presented on [[views.html.frontend.celebrity_storefront_finalize]]
 *
 * @param name the buyer's name
 * @param email the buyer's email address
 * @param postalCode the buyer's postalcode
 * @param paymentToken the payment token representing the buyer's card as
 *     provided by Stripe.
 * @param paymentApiKey our publishable API key from Stripe.
 * @param paymentJsModule the javascript module that should be used to process
 *   payments. This is either "stripe-payment" or "yes-maam-payment".
 * @param editUrl Link to the payment information form.
 */
case class FinalizeBillingViewModel (
  name: String,
  email: String,
  postalCode: String,
  paymentToken: String,
  paymentApiKey: String,
  paymentJsModule: String,
  editUrl: String
)