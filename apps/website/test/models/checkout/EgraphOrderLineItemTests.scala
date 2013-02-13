package models.checkout

import utils.{DBTransactionPerTest, DateShouldMatchers, EgraphsUnitTest}
import models.checkout.checkout.Conversions._
import models.enums._
import services.AppConfig


class EgraphOrderLineItemTests extends EgraphsUnitTest
  with LineItemTests[EgraphOrderLineItemType, EgraphOrderLineItem]
  with SavesAsLineItemEntityThroughServicesTests[EgraphOrderLineItem, EgraphOrderLineItemServices]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  import LineItemTestData._

  override lazy val newItemType: EgraphOrderLineItemType = randomEgraphOrderType

  override def resolutionBlockingTypes: Seq[LineItemTypes] = Nil

  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(SubtotalLineItemType, BalanceLineItemType),
    Seq(TotalLineItemType, randomCouponType(), randomTaxType)
  )

  override def resolvableItemSets: Seq[LineItems] = Seq(
    Nil,
    Seq(randomGiftCertificateItem, randomSubtotalItem)
  )

  override def restoreLineItem(id: Long) = lineItemStore.findByIdWithCodeType(id, EgraphOrderLineItemType.codeType)

  override def hasExpectedRestoredDomainObject(lineItem: EgraphOrderLineItem) = {
    val restoredOrder = lineItem.domainObject
    val initialOrder = newItemTypeResolved.domainObject

    restoredOrder.productId should be (initialOrder.productId)
    restoredOrder.recipientName should be (initialOrder.recipientName)
    restoredOrder.requestedMessage should be (initialOrder.requestedMessage)
    restoredOrder.messageToCelebrity should be (initialOrder.messageToCelebrity)
    restoredOrder.paymentStatus should be (PaymentStatus.Charged)

    true
  }

  def newItemTypeResolved = newItemType.lineItems(Nil, Nil).get.head
}
