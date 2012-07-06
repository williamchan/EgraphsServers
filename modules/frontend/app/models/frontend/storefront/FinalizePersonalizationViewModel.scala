package models.frontend.storefront

/**
 * ViewModel for the personalization information as presented on the finalize order page.
 * See [[views.frontend.html.celebrity_storefront_finalize]]
 *
 * @param celebName the celebrity's public name. (e.g. David Price)
 * @param productTitle the product's title (e.g. "MLB Finals 2012")
 * @param recipientName the egraph recipient's name
 * @param messageText The requested text for the message, or a replacement
 *   for that text if no message was requested. For example, if no message was requested
 *   it could say "His signature only"
 * @param editUrl link to the personalization form.
 */
case class FinalizePersonalizationViewModel (
  celebName: String,
  productTitle: String,
  recipientName: String,
  messageText: String,
  editUrl: String
)
