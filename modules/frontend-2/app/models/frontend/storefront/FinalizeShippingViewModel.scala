package models.frontend.storefront

/**
 * ViewModel for shipping information as provided on the
 * Finalize page.
 *
 * See [[views.html.frontend.celebrity_storefront_finalize]]
 *
 * @param name the shipping recipient's name
 * @param email the shipping recipient's email address
 * @param addressLine1 first line of the shipping recipient's address
 * @param addressLine2 optional second line of the address
 * @param city the city being shipped to (e.g. Florence)
 * @param state the state being shipped to (e.g. Washington)
 * @param postalCode the postal code (e.g. 98122)
 * @param editUrl link to edit order shipping information.
 */
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

