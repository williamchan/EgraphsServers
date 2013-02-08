package models.checkout

import checkout.Conversions._
import utils._
import services.AppConfig
import models.enums.CheckoutCodeType
import LineItemTestData._


class CashTransactionLineItemTests extends EgraphsUnitTest
  with LineItemTests[CashTransactionLineItemType, CashTransactionLineItem]
  with CanInsertAndUpdateAsThroughServicesWithLineItemEntityTests[CashTransactionLineItem]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  //
  // LineItemTests members
  //
  override lazy val newItemType: CashTransactionLineItemType = randomCashTransactionType

  override def resolvableItemSets: Seq[LineItems] = Seq(
    Seq(randomBalanceItem)
  )

  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(BalanceLineItemType)
  )

  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(TotalLineItemType),
    Seq(TotalLineItemType, SubtotalLineItemType)
  )



  override def restoreLineItem(id: Long): Option[CashTransactionLineItem] = {
    AppConfig.instance[LineItemStore].findByIdWithCodeType(id, CheckoutCodeType.CashTransaction)
  }

  override def hasExpectedRestoredDomainObject(lineItem: CashTransactionLineItem): Boolean = {
    val txn = lineItem.domainObject

    txn.id should not be (0)
    txn.accountId should be (checkout.accountId)
    txn.stripeCardTokenId should be (newItemType.stripeCardTokenId)
    txn.lineItemId should be (Some(lineItem.id))

    true
  }
}