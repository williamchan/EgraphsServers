package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}

case class TotalLineItem (
  amount: Money
) extends LineItem[Money] {

  override def subItems = Nil
  override val itemType = TotalLineItemType

  override def toJson = {
    // TODO(SER-499): implement
    ""
  }

  // NOTE(SER-499): unused
  override val id: Long = checkout.Unpersisted
  override def domainObject = amount
  override val _domainEntityId: Long = checkout.UnusedDomainEntity
  override def transact = this
  override def withCheckoutId(newCheckoutId: Long) = this
}

object TotalLineItemType extends LineItemType[Money] {
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
    def isNeededType(itemType: LineItemType[_]) =  {
      import LineItemNature._
      Seq(Summary, Discount, Tax, Fee).contains(itemType.nature)
    }

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
