package models.checkout

import checkout.Conversions._
import utils._
import utils.TestData._
import services.AppConfig
import services.Finance.TypeConversions._
import services.payment.StripeTestPayment
import models.enums.CodeType


class CashTransactionLineItemTests extends EgraphsUnitTest
  with LineItemTests[CashTransactionLineItemType, CashTransactionLineItem]
  with CanInsertAndUpdateAsThroughServicesWithLineItemEntityTests[CashTransactionLineItem]
  with ClearsCacheAndBlobsAndValidationBefore
  with DateShouldMatchers
  with DBTransactionPerTest
{
  //
  // LineItemTests members
  //
  override lazy val newItemType: CashTransactionLineItemType = {
    CashTransactionLineItemType(newSavedAccount().id, Some("98888"), Some(payment.testToken().id))
  }

  override def resolvableItemSets: Seq[LineItems] = Seq(
    Seq(randomBalance)
  )

  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(BalanceLineItemType)
  )

  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(TotalLineItemType),
    Seq(TotalLineItemType, SubtotalLineItemType)
  )



  override def restoreLineItem(id: Long): Option[CashTransactionLineItem] = {
    AppConfig.instance[LineItemStore].findByIdOfCodeType(id, CodeType.CashTransaction)
  }

  override def hasExpectedRestoredDomainObject(lineItem: CashTransactionLineItem): Boolean = {
    val txn = lineItem.domainObject

    txn.id should not be (0)
    txn.accountId should be (checkout.accountId)
    txn.stripeCardTokenId should be (newItemType.stripeCardTokenId)
    txn.lineItemId should be (Some(lineItem.id))

    true
  }




  //
  // Helpers
  //
  def payment: StripeTestPayment = {
    val pment = AppConfig.instance[StripeTestPayment]; pment.bootstrap();
    pment
  }

  def randomBalance = BalanceLineItem(randomMoney)

  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()
}
