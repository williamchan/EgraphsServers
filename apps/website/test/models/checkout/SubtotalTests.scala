package models.checkout

import utils.{DBTransactionPerTest, ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import utils.TestData._
import LineItemMatchers._
import org.joda.money.Money
import models.checkout.checkout.Conversions._
import services.Finance.TypeConversions._

class SubtotalTests extends EgraphsUnitTest
  with LineItemTests[SubtotalLineItemType, SubtotalLineItem]
  with ClearsCacheAndBlobsAndValidationBefore
  with DBTransactionPerTest
{
  
  //
  // Test cases
  //
  "SubtotalLineItemType" should "create a SubtotalLineItem for the correct ammount" in {
    // TODO: add more products, refunds to this
    val items = generateSeqsFrom(randomGiftCertItem, 2).flatten
    val subtotal = SubtotalLineItemType.lineItems(items, Nil).get.head

    subtotal should haveAmount(items.sumAmounts)
  }


  //
  // LineItemTests members
  //
  override def newItemType = SubtotalLineItemType
  override def resolvableItemSets: Seq[LineItems] = generateSeqsFrom(randomGiftCertItem)
  override def resolutionBlockingTypes: Seq[LineItemTypes] = generateSeqsFrom(randomGiftCertType)
  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    otherSummaryTypes, taxTypes, taxTypes ++ otherSummaryTypes
  )

  override def restoreLineItem(id: Long) = Some(SubtotalLineItem(randomMoney))
  override def hasExpectedRestoredDomainObject(lineItem: SubtotalLineItem) = {
    lineItem.amount == lineItem.domainObject
  }
  
  //
  // Data helpers
  //
  def generateSeqsFrom[T](gen: => T, n: Int = 2) = {
    for(i <- 0 to n) yield { for (j <- 0 to i) yield gen }.toSeq
  }.toSeq

  def randomGiftCertItem = randomGiftCertType.lineItems().get.head
  def randomGiftCertType = GiftCertificateLineItemType(generateFullname, randomMoney)
  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()

  def otherSummaryTypes = Seq(TotalLineItemType, BalanceLineItemType)
  def taxTypes = Seq(taxType(), taxType(0.05))

  def taxType(rate: Double = 0.09) = TaxLineItemType("98888", BigDecimal(rate), Some("Test tax"))
}
