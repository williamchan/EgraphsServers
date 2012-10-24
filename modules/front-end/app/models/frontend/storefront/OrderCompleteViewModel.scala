package models.frontend.storefront

import java.util.Date
import org.joda.money.Money

/**
 * Viewmodel for rendering the order complete page.
 *
 * See [[views.html.frontend.celebrity_storefront_complete]]
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
 * @param expectedDeliveryDate the date by which the egraph is expected
 *                             to deliver. This can be found on the order's `inventoryBatch`
 * @param faqHowLongLink link to FAQ section on expected delivery dates
 * @param hasPrintOrder whether a physical print was ordered
 * @param withAffiliateMarketing whether to notify affiliate marketing partners about this order
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
  expectedDeliveryDate: Date,
  faqHowLongLink: String,
  hasPrintOrder: Boolean,
  withAffiliateMarketing: Boolean = false
)
