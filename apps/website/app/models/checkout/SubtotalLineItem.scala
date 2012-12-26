package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}

case class SubtotalLineItem (
  amount: Money
) extends LineItem[Money] {

  override def subItems = Nil
  override val itemType = SubtotalLineItemType

  override def toJson = {
    // TODO(SER-499): implement, use JSON type
    ""
  }


  // NOTE(SER-499): unused
  override val id: Long = checkout.Unpersisted
  override def domainObject = amount
  override def transact = this
  override def withCheckoutId(newCheckoutId: Long) = this
}

abstract class SubtotalLineItemType extends LineItemType[Money]
object SubtotalLineItemType extends SubtotalLineItemType {
  override val id = checkout.Unpersisted
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
  override def lineItems(
    resolvedItems: Seq[LineItem[_]],
    pendingResolution: Seq[LineItemType[_]]
  ): Seq[SubtotalLineItem] = {
    def isProduct(itemType: LineItemType[_]) = itemType.nature == LineItemNature.Product

    pendingResolution.filter(isProduct(_)) match {
      case Seq(_) => Nil
      case Nil => Seq {
        val amount: Money = resolvedItems.foldLeft (Money.zero(CurrencyUnit.USD)) {
          (acc, next) =>
            if (isProduct(next.itemType)) next.amount plus acc.getAmount
            else acc
        }
        new SubtotalLineItem(amount)
      }
    }
  }
}