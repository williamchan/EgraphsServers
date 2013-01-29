package models.checkout

import utils.{DBTransactionPerTest, EgraphsUnitTest}
import LineItemMatchers._
import models.checkout.checkout.Conversions._
import LineItemTestData._

class SubtotalTests extends EgraphsUnitTest
  with LineItemTests[SubtotalLineItemType, SubtotalLineItem]
  with DBTransactionPerTest
{ 
  //
  // Test cases
  //
  "SubtotalLineItemType" should "create a SubtotalLineItem for the correct ammount" in {
    // TODO: add new products, refunds to this when they're implemented
    val items = resolvableItemSets.flatten
    val subtotal = SubtotalLineItemType.lineItems(items, Nil).get.head

    subtotal should haveAmount(items.sumAmounts)
  }


  //
  // LineItemTests members
  //
  override def newItemType = SubtotalLineItemType
  override def resolvableItemSets: Seq[LineItems] = Seq(
    seqOf(randomGiftCertificateItem)(1),
    seqOf(randomGiftCertificateItem)(3)
  )
  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    seqOf(randomGiftCertificateType)(1),
    seqOf(randomGiftCertificateType)(3)
  )
  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    otherSummaryTypes, twoTaxTypes, twoTaxTypes ++ otherSummaryTypes
  )

  override def restoreLineItem(id: Long) = Some(SubtotalLineItem(randomMoney))
  override def hasExpectedRestoredDomainObject(lineItem: SubtotalLineItem) = {
    lineItem.amount == lineItem.domainObject
  }

  
  //
  // SubtotalTests members
  //
  def otherSummaryTypes = Seq(TotalLineItemType, BalanceLineItemType)
  def twoTaxTypes = seqOf(randomTaxType)(2)
}
