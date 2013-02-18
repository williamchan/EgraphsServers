package models.checkout

import utils.{DBTransactionPerTest, DateShouldMatchers, EgraphsUnitTest}
import models.checkout.checkout.Conversions._
import models.enums._
import models.checkout.Checkout.CheckoutFailedShippingAddressMissing
import LineItemTestData._
import CheckoutScenario._
import RichCheckoutConversions._


class EgraphOrderLineItemTests extends EgraphsUnitTest
  with LineItemTests[EgraphOrderLineItemType, EgraphOrderLineItem]
  with SavesAsLineItemEntityThroughServicesTests[EgraphOrderLineItem, EgraphOrderLineItemServices]
  with CheckoutTestCases
  with DateShouldMatchers
  with DBTransactionPerTest
{


  //
  // LineItemTests and SavesAsLineItemEntityThroughServicesTests
  //
  override lazy val newItemType: EgraphOrderLineItemType = randomEgraphOrderType()

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
    val initialOrder = newLineItem.domainObject

    restoredOrder.productId should be (initialOrder.productId)
    restoredOrder.recipientName should be (initialOrder.recipientName)
    restoredOrder.requestedMessage should be (initialOrder.requestedMessage)
    restoredOrder.messageToCelebrity should be (initialOrder.messageToCelebrity)
    restoredOrder.paymentStatus should be (PaymentStatus.Charged)
    true
  }




  //
  // CheckoutTestCases
  //
  override lazy val scenarios = Seq(
    egraphScenario,
    printScenario,
    CheckoutScenario(
      Seq(randomEgraphOrderType(), randomGiftCertificateType),
      Seq(randomGiftCertificateType)
    ),
    CheckoutScenario(
      Seq(randomEgraphOrderType(), randomEgraphOrderType()),
      buyer = None
    ),
    CheckoutScenario(
      Seq(randomEgraphOrderType()),
      cashTransaction = None
    )
  )

  lazy val egraphScenario = CheckoutScenario(
    Seq(randomEgraphOrderType())
  )

  lazy val printScenario = CheckoutScenario(
    Seq(randomEgraphOrderType(true))
  )

  lazy val printScenarioWithoutAddress = CheckoutScenario(
    Seq(randomEgraphOrderType(true)),
    address = None
  )




  //
  // EgraphOrderLineItemType tests
  //
  "An Egraph Order with framed print" should "generate a PrintOrderLineItem" in printScenario { implicit scenario =>
    val prints = initialCheckout.lineItems.ofCodeType(CheckoutCodeType.PrintOrder)
    prints.size should be (1)
  }

  it should "throw a MissingRequireAddressException if transacted without a shipping address" in printScenarioWithoutAddress {
    implicit scenario =>

    initialCheckout.transact(Some(randomCashTransactionType)) match {
      case Left(failure: CheckoutFailedShippingAddressMissing) =>
      case Left(failure) => fail("Wrong failure case: " + failure)
      case Right(checkout) => fail("Did not fail though expected to.")
    }
  }




}
