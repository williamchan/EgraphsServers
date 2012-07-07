package models.frontend.storefront

import java.util.Date
import org.joda.money.Money

/**
 * Viewmodel for rendering the order complete page.
 *
 * See [[views.frontend.html.celebrity_storefront_complete]]
 *
 * @param orderDate the date the order was made
 * @param orderNumber the order number / id
 * @param buyerName the buyer's name
 * @param buyerEmail the buyer's emial address
 * @param ownerName the recipient's name
 * @param ownerEmail the recipient's email address
 * @param celebName the celebrity' name being purchased from
 * @param productName the name of the photo that was purchased
 * @param totalPrice the total cost of the order
 * @param guaranteedDeliveryDate the date by which the egraph is guaranteedn
 *   to deliver. This can be found on the order's `inventoryBatch`
 */
case class OrderCompleteViewModel (
  orderDate: Date,
  orderNumber: Long,
  buyerName: String,
  buyerEmail: String,
  ownerName: String,
  ownerEmail: String,
  celebName: String,
  productName: String,
  totalPrice: Money,
  guaranteedDeliveryDate: Date
)