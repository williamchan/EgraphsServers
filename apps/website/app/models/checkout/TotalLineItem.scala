package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}

case class TotalLineItem (
  amount: Money
) extends LineItem[Money] {

  override def itemType = TotalLineItemType
  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override def toJson = {
    // TODO(SER-499): implement
    ""
  }

  // NOTE(SER-499): unused
  override val id: Long = checkout.Unpersisted
  override def domainObject = amount
  override def transact(checkoutId: Long) = this
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
   * Sums the taxes, fees
   * @param resolvedItems
   * @param pendingResolution
   * @return Seq(new line items) if the line item type was successfully applied.
   *         Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  override def lineItems(
    resolvedItems: Seq[LineItem[_]],
    pendingResolution: Seq[LineItemType[_]]
  ): Seq[TotalLineItem] = {
    def isNeededItem(item: LineItem[_]) = isNeededType(item.itemType)
    def isNeededType(itemType: LineItemType[_]) = itemType.nature != LineItemNature.Summary

    pendingResolution.find(isNeededType) match {
      case None =>
        val totalAmount = resolvedItems.foldLeft(Money.zero(CurrencyUnit.USD)) { (acc, next) =>
          if (isNeededItem(next)) acc plus next.amount else acc
        }

        Seq(TotalLineItem(totalAmount))
      case _ => Nil

    }
    // Want to sum subtotal, discounts, tax, and fees

  }
}
