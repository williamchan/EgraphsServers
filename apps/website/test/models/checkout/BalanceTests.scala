package models.checkout

import utils.{DBTransactionPerTest, ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import org.joda.money.Money
import LineItemMatchers._
import utils.TestData._
import services.Finance.TypeConversions._
import checkout.Conversions._
import models.CashTransaction


class BalanceTests extends EgraphsUnitTest
  with LineItemTests[BalanceLineItemType, BalanceLineItem]
  with ClearsCacheAndBlobsAndValidationBefore
  with DBTransactionPerTest
{
  //
  // Test cases
  //
  "BalanceLineItemType" should "create a BalanceLineItem of the right amount" in {
    val items = randomTotal +: generatePayments(3)
    val balance = BalanceLineItemType.lineItems(items, Nil).get.head

    balance should haveAmount(items.sumAmounts)
  }



  //
  // LineItemTests members
  //
  override def newItemType =  BalanceLineItemType
  override def resolvableItemSets: Seq[LineItems] = Seq(oneTotal, oneTotal ++ generatePayments(2))
  override def resolutionBlockingTypes: Seq[LineItemTypes] = Seq(
    Seq(TotalLineItemType),
    Seq(CashTransactionLineItemType(0L, None, None))
  )
  override def nonResolutionBlockingTypes: Seq[LineItemTypes] = Seq(Nil) // could be anything not in blocking types above


  override def restoreLineItem(id: Long) = Some(BalanceLineItem(randomMoney))
  override def hasExpectedRestoredDomainObject(lineItem: BalanceLineItem) = {
    lineItem.amount == lineItem.domainObject
  }



  //
  // Data helpers
  //
  def generatePayments(n: Int) = { for (_ <- 0 to n) yield randomPayment }.toSeq
  def randomPayment = new CashTransactionLineItem(
    _entity = LineItemEntity(randomMoney.negated, "test entity"),
    _typeEntity = CashTransactionLineItemType.paymentEntity,
    _maybeCashTransaction = Some(CashTransaction())
  )

  def oneTotal = Seq(randomTotal)
  def randomTotal = TotalLineItem(randomMoney)

  def randomMoney = BigDecimal(random.nextInt(200)).toMoney()
}
