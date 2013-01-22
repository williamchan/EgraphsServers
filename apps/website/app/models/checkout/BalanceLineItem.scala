package models.checkout

import checkout._
import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}
import services.Finance.TypeConversions._

/**
 * Created with IntelliJ IDEA.
 * User: kevin
 * Date: 1/10/13
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
case class BalanceLineItem(amount: Money) extends LineItem[Money] {

  override def itemType = BalanceLineItemType
  override def withAmount(newAmount: Money) = this.copy(newAmount)

  override def toJson = {
    // TODO(SER-499): implement
    ""
  }

  //
  // NOTE(SER-499): Unused
  //
  override val id: Long = checkout.Unpersisted
  override def domainObject = amount
  override def transact(checkout: Checkout) = this
  override def checkoutId = checkout.Unpersisted
  override def withCheckoutId(newCheckoutId: Long) = this
  override def subItems = Nil

}

abstract class BalanceLineItemType extends LineItemType[Money]
object BalanceLineItemType extends BalanceLineItemType {
  override val id = checkout.Unpersisted
  override val description = "Balance"
  override val nature = LineItemNature.Summary
  override val codeType = CodeType.Balance
  override val toJson = ""


  override def lineItems(resolved: LineItems, pendingResolution: LineItemTypes) = {
    import LineItemNature._

    if (pendingResolution.ofNatures(Payment, Summary).isEmpty) {
       Some {
        val total = resolved(CodeType.Total).head
        val payments = resolved(Payment)
        val difference = total.amount plus payments.sumAmounts // payments to us are negative
        Seq(BalanceLineItem(difference))
      }
    } else {
      None
    }
  }
}