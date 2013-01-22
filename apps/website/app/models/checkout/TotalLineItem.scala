package models.checkout

import checkout._
import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}

case class TotalLineItem (amount: Money) extends LineItem[Money] {

  override def itemType = TotalLineItemType
  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override def toJson = {
    // TODO(SER-499): implement
    ""
  }


  //
  // NOTE(SER-499): unused
  //
  override val id: Long = checkout.Unpersisted
  override def domainObject = amount
  override def transact(checkout: Checkout) = this
  override def checkoutId = checkout.Unpersisted
  override def withCheckoutId(newCheckoutId: Long) = this
  override def subItems = Nil

}


abstract class TotalLineItemType extends LineItemType[Money]
object TotalLineItemType extends TotalLineItemType {
  override val id = checkout.Unpersisted
  override val description = "Total"
  override val nature = LineItemNature.Summary
  override val codeType = CodeType.Total
  override val toJson = ""


  /**
   * Sums everything except the subtotal. (Could use subtotal in place of products and refunds, but
   * re-summing over the products seems more robust against errors.)
   *
   * @param resolvedItems
   * @param pendingTypes
   * @return Seq(new line items) if the line item type was successfully applied.
   *         Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  override def lineItems(resolvedItems: LineItems, pendingTypes: LineItemTypes) = {
    import LineItemNature._

    def pendingNeeded = pendingTypes.ofNatures(Tax, Fee)
    def maybeSubtotal = resolvedItems(CodeType.Subtotal).headOption.map(_.amount)

    (pendingNeeded, maybeSubtotal) match {
      case (Nil, Some(subtotal: Money)) => Some {
        val taxesAndFees = resolvedItems.ofNatures(Tax, Fee).sumAmounts
        Seq(TotalLineItem(taxesAndFees plus subtotal))
      }
      case _ => None
    }
  }
}
