package models.checkout

import org.joda.money.Money
import models.checkout.checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}

case class BalanceLineItem(amount: Money) extends LineItem[Money] {

  override def itemType = BalanceLineItemType
  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override def toJson = jsonify("Balance", "Amount due to complete transaction")


  override val id: Long = -1
  override def domainObject = amount
  override def transact(checkout: Checkout) = this
  override def checkoutId = -1
  override def withCheckoutId(newCheckoutId: Long) = this


}

abstract class BalanceLineItemType extends LineItemType[Money]
case object BalanceLineItemType extends BalanceLineItemType {
  override val id: Long = -1L
  override val description = "Balance"
  override val nature = LineItemNature.Summary
  override val codeType = CheckoutCodeType.Balance

  override def lineItems(resolved: LineItems, pendingResolution: LineItemTypes) = {
    import LineItemNature._

    if (pendingResolution.ofNatures(Payment, Summary).isEmpty) {
       Some {
        val total = resolved(CheckoutCodeType.Total).head
        val payments = resolved(Payment)
        val difference = total.amount plus payments.sumAmounts // payments to us are negative
        Seq(BalanceLineItem(difference))
      }
    } else {
      None
    }
  }
}