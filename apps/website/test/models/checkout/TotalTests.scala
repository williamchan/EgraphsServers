package models.checkout

import utils.{DBTransactionPerTest, EgraphsUnitTest}
import LineItemMatchers._
import models.checkout.checkout.Conversions._
import LineItemTestData._


class TotalTests extends EgraphsUnitTest
  with LineItemTests[TotalLineItemType, TotalLineItem]
  with DBTransactionPerTest
{

  //
  // Test cases
  //
  "TotalLineItemType" should "create a TotalLineItem for correct amount" in new EgraphsTestApplication {
    // TODO(fees): update with fees
    // TODO(discounts): update with discounts
    val total = TotalLineItemType.lineItems(subtotalWithTaxes, Nil).get.head

    total should haveAmount(subtotalWithTaxes.sumAmounts)
  }







  //
  // LineItemTests members
  //
  override def newItemType = TotalLineItemType
  override def resolvableItemSets: Seq[LineItems] = Seq(Seq(subtotal), subtotalWithTax, subtotalWithTaxes)
  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(oneTaxType, subtotalWithTaxTypes)
  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = {
    val otherSummaries = Seq(TotalLineItemType, BalanceLineItemType)
    val withCashTxn = CashTransactionLineItemType(0L, None, None) +: otherSummaries
    Seq(otherSummaries, withCashTxn)
  }

  override def restoreLineItem(id: Long) = Some(TotalLineItem(randomMoney))
  override def hasExpectedRestoredDomainObject(lineItem: TotalLineItem) = {
    lineItem.amount == lineItem.domainObject
  }




  //
  // Data helpers
  //
  lazy val subtotal: SubtotalLineItem = randomSubtotalItem
  lazy val subtotalWithTax: LineItems =  Seq(randomTaxItem, subtotal)
  lazy val subtotalWithTaxes: LineItems = seqOf(randomTaxItem)(2) ++ subtotalWithTax
  lazy val oneTaxType: LineItemTypes = Seq(randomTaxType)
  lazy val subtotalWithTaxTypes: LineItemTypes = Seq(randomTaxType, SubtotalLineItemType)
}
