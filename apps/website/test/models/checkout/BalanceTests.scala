package models.checkout

import utils.{DBTransactionPerTest, EgraphsUnitTest}
import LineItemMatchers._
import checkout.Conversions._
import LineItemTestData._


class BalanceTests extends EgraphsUnitTest
  with LineItemTests[BalanceLineItemType, BalanceLineItem]
  with DBTransactionPerTest
{
  //
  // Test cases
  //
  "BalanceLineItemType" should "create a BalanceLineItem of the right amount" in {
    val items = randomTotalItem +: seqOf(randomCashTransactionItem)(3)
    val balance = BalanceLineItemType.lineItems(items, Nil).get.head

    balance should haveAmount(items.sumAmounts)
  }



  //
  // LineItemTests members
  //
  override def newItemType =  BalanceLineItemType
  override def resolvableItemSets: Seq[LineItems] = Seq(oneTotal, totalWithTwoPayments)
  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(TotalLineItemType),
    Seq(randomCashTransactionType)
  )

  // Could be anything but total or payment, but generally nothing should be left unresolved anyway
  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(Nil)


  override def restoreLineItem(id: Long) = Some(BalanceLineItem(randomMoney))
  override def hasExpectedRestoredDomainObject(lineItem: BalanceLineItem) = {
    lineItem.amount == lineItem.domainObject
  }



  //
  // BalanceTests members
  //
  def oneTotal = Seq(randomTotalItem)
  def totalWithTwoPayments = oneTotal ++ seqOf(randomCashTransactionItem)(2)
}
