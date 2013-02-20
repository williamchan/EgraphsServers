package models.checkout

import models.checkout.checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}
import org.joda.money.Money
import play.api.libs.json.Json

/**
 * Summary item for the sum of all charges and credits incurred by the customer (e.g. everything,
 * positive or negative, except for payments). This definition is flexible and may shift a bit
 * when refunds are implemented, but the idea is tied closely to the balance;
 * total + payments = balance, and when the balance is zero everything should be paid for exactly.
 *
 * @param amount
 */
case class TotalLineItem (amount: Money) extends LineItem[Money] {

  override def itemType = TotalLineItemType
  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override def toJson = jsonify("Total", "Final cost of purchase")

  override val id: Long = -1
  override def domainObject = amount
  override def transact(checkout: Checkout) = this
  override def checkoutId = -1
  override def withCheckoutId(newCheckoutId: Long) = this


}


abstract class TotalLineItemType extends LineItemType[Money]
object TotalLineItemType extends TotalLineItemType {
  override val id: Long = -1L
  override val description = "Total"
  override val nature = LineItemNature.Summary
  override val codeType = CheckoutCodeType.Total


  /**
   * Sums everything except the subtotal. (Could use subtotal in place of products and refunds, but
   * re-summing over the products seems more robust against errors.)
   *
   * Singleton since there is no domain data needed to create it.
   *
   * @param resolvedItems
   * @param pendingTypes
   * @return Seq(new line items) if the line item type was successfully applied.
   *         Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  override def lineItems(resolvedItems: LineItems, pendingTypes: LineItemTypes) = {
    import LineItemNature._

    def pendingNeeded = pendingTypes.ofCodeType(CheckoutCodeType.Subtotal) ++ pendingTypes.ofNatures(Tax, Fee, Discount)
    def maybeSubtotal = resolvedItems(CheckoutCodeType.Subtotal).headOption.map(_.amount)

    (pendingNeeded, maybeSubtotal) match {
      case (Nil, Some(subtotal: Money)) => Some {
        val taxesFeesDiscounts = resolvedItems.ofNatures(Tax, Fee, Discount).sumAmounts
        Seq(TotalLineItem(taxesFeesDiscounts plus subtotal))
      }
      case _ => None
    }
  }
}
