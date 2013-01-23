package models.checkout

import models.checkout.checkout.Conversions._
import models.enums.{CodeType, LineItemNature}
import org.joda.money.{CurrencyUnit, Money}

case class SubtotalLineItem (
  amount: Money
) extends LineItem[Money] {

  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override val itemType = SubtotalLineItemType

  override def toJson = {
    // TODO(SER-499): implement, use JSON type
    ""
  }


  //
  // NOTE(SER-499): unused
  //
  override val id: Long = -1
  override def domainObject = amount
  override def transact(checkout: Checkout) = this
  override def checkoutId = -1
  override def withCheckoutId(newCheckoutId: Long) = this
  override def subItems = Nil
}

abstract class SubtotalLineItemType extends LineItemType[Money]
object SubtotalLineItemType extends SubtotalLineItemType {
  override val id: Long = -1L
  override val description = "Subtotal"
  override val nature = LineItemNature.Summary
  override val codeType = CodeType.Subtotal
  override val toJson = ""

  /**
   * Sums all the products and creates a SubtotalLineItem for that amount, unless there are products
   * pending resolution; then does nothing.
   * @param resolvedItems
   * @param pendingResolution
   * @return Seq(new line items) if the line item type was successfully applied.
   *         Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = {
    import LineItemNature._
    // TODO(refunds): at this point, it may be good if refunds of specific items have been applied or are factored into calculations here

    if (!pendingResolution.ofNatures(Product, Refund).isEmpty) { None }
    else Some { Seq {
      val amount: Money = resolvedItems.ofNatures(Product, Refund).sumAmounts
      new SubtotalLineItem(amount)
    }}
  }
}