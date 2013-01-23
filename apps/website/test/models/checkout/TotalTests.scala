package models.checkout

import org.joda.money.Money
import utils.{ClearsCacheAndBlobsAndValidationBefore, DBTransactionPerTest, EgraphsUnitTest}
import utils.TestData._
import services.Finance.TypeConversions._
import LineItemMatchers._
import models.checkout.checkout.Conversions._


class TotalTests extends EgraphsUnitTest
  with LineItemTests[TotalLineItemType, TotalLineItem]
  with ClearsCacheAndBlobsAndValidationBefore
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
  lazy val subtotal: SubtotalLineItem = randomSubtotal
  lazy val subtotalWithTax: LineItems =  Seq(tax(subtotal), subtotal)
  lazy val subtotalWithTaxes: LineItems = {
    tax(subtotal, 0.05) +: (tax(subtotal, 0.0333) +: subtotalWithTax)
  }

  lazy val oneTaxType: LineItemTypes = Seq(taxType())
  lazy val subtotalWithTaxTypes: LineItemTypes = Seq(taxType(), SubtotalLineItemType)

  def randomSubtotal: SubtotalLineItem = SubtotalLineItem(randomMoney)

  def tax(onSubtotal: SubtotalLineItem, rate: Double = 0.09): TaxLineItem = {
    taxType(rate).lineItems(Seq(onSubtotal), Nil).get.head.asInstanceOf[TaxLineItem]
  }

  def taxType(rate: Double = 0.09): TaxLineItemType = {
    TaxLineItemType("98888", BigDecimal(rate), Some("Test tax"))
  }

  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()
}
