package models.checkout

import checkout.Conversions._
import utils.{DateShouldMatchers, DBTransactionPerTest, EgraphsUnitTest, TestData}


class PrintOrderLineItemTests extends EgraphsUnitTest
  with LineItemTests[PrintOrderLineItemType, PrintOrderLineItem]
  with SavesAsLineItemEntityThroughServicesTests[PrintOrderLineItem, PrintOrderLineItemServices]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  import LineItemTestData._
  import TestData._

  override lazy val newItemType: PrintOrderLineItemType = randomPrintOrderType

  override def resolutionBlockingTypes: Seq[LineItemTypes] = Nil

  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(SubtotalLineItemType, BalanceLineItemType),
    Seq(TotalLineItemType, randomCouponType(), randomTaxType)
  )

  override def resolvableItemSets: Seq[LineItems] = Seq(
    Nil,
    Seq(randomGiftCertificateItem, randomSubtotalItem)
  )

  override def restoreLineItem(id: Long) = lineItemStore.findByIdWithCodeType(id, PrintOrderLineItemType.codeType)

  override def hasExpectedRestoredDomainObject(lineItem: PrintOrderLineItem) = {
    val restoredPrint = lineItem.domainObject
    val initialPrint = newLineItem.domainObject

    restoredPrint.orderId should be (initialPrint.orderId)
    restoredPrint.id should be > (0L)
    restoredPrint.lineItemId should be ('defined)
    restoredPrint.lineItemId.get should be > (0L)

    true
  }
}
