package models.checkout

import org.joda.money.Money
import models.checkout.checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}

/**
 * Represents the amount due on the checkout it belongs to. Ultimately, its use is for determining the amount to
 * actually charge (or, when supported, refund) to a customer.
 *
 * In general is equal to the difference between the total
 * and payments (line items for payments to us are < 0, so the computation is actually total + payments).
 *
 * So, amount should be:
 *   - Equal to the total before being transacted the first time
 *   - Zero after transaction.
 *   - Equal to amount of added types when pending an update (e.g. transacted, then new types added with nonzero amounts).
 */
case class BalanceLineItem(amount: Money) extends LineItem[Money] {

  override def itemType = BalanceLineItemType
  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override def toJson = jsonify("Balance", "Amount due to complete transaction")


  /**
   * These fields are unused for now in this and other Summary LineItems. Although they could be removed from the
   * LineItem trait, it would cause more pain in the common case by requiring matching/casting when accessing
   * generically. Could be refactored out later if there is a painless way to do so.
   */
  override val id: Long = -1
  override def domainObject = amount
  override def transact(checkout: Checkout) = this // should not be persisted.
  override def checkoutId = -1
  override def withCheckoutId(newCheckoutId: Long) = this


}

abstract class BalanceLineItemType extends LineItemType[Money]
case object BalanceLineItemType extends BalanceLineItemType {
  override val description = "Balance"
  override val nature = LineItemNature.Summary
  override val codeType = CheckoutCodeType.Balance

  /** unused */
  override val id: Long = -1L

  override def lineItems(resolved: LineItems, pendingResolution: LineItemTypes) = {
    import LineItemNature._

    if (pendingResolution.ofNatures(Payment, Summary).isEmpty) Some {
      val total = resolved(CheckoutCodeType.Total).head
      val payments = resolved(Payment)
      val difference = total.amount plus payments.sumAmounts // payments to us are negative
      Seq(BalanceLineItem(difference))
    } else {
      None
    }
  }
}