package models.checkout

import utils._
import org.joda.money.Money
import org.scalatest.matchers.{MatchResult, Matcher}
import services.AppConfig
import services.Finance.TypeConversions._
import services.payment.StripeTestPayment
import LineItemMatchers._
import models.checkout.checkout.Conversions._
import models.checkout.Checkout._
import models.enums.LineItemNature._
import TestData._


class CheckoutTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesWithLongKeyTests[Checkout, CheckoutEntity]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  //
  // CanInsertAndUpdateAsThroughServicesTests members
  //
  override def newModel: Checkout = Checkout(Seq(giftCertificateTypeForFriend), taxedZip, Some(newSavedCustomer()))
  override def saveModel(toSave: Checkout): Checkout = toSave.transact(cashTxnTypeFor(toSave)) match {
    case Right(checkout: Checkout) => checkout
    case Left(failure) => fail("saveModel failed with " + failure)
  }
  override def restoreModel(id: Long): Option[Checkout] = checkoutServices.findById(id)

  override def transformModel(toTransform: Checkout) = {
    toTransform.withAdditionalTypes(Seq(giftCertificateTypeForShittyFriend))
  }



  //
  // Checkout test cases
  //
  "A checkout" should "fail to transact without a customer" in {
    val checkout: Checkout = Checkout(oneGiftCertificate, taxedZip, None)
    lazy val failedTransaction: FailureOrCheckout = checkout.transact(randomCashTxnType)

    failedTransaction should be ('left)

    failedTransaction match {
      case Left(error: FailedCheckoutWithCharge) =>
        error.charge should be ('defined)
        error.charge.get.refunded should be (true)
      case Left(other: CheckoutFailed) => fail("Expected error with charge, received " + other.getClass)
    }
  }

  it should "have a balance of zero after transaction" in {
    val transacted: Checkout = saveModel(newModel)
    transacted.total should haveNegatedAmountOf (transacted.payments.head)
    transacted.balance should haveAmount (zeroDollars)
  }

  it should "charge the customer for the total on checkout" in {
    val total: TotalLineItem = newModel.total
    val transacted: Checkout = saveModel(newModel)
    val txnItem = transacted.payments.head

    txnItem should haveNegatedAmountOf (total)
  }

  it should "not have duplicate summaries" in {
    val taxedCheckout: Checkout = Checkout(oneGiftCertificate, taxedZip, Some(newSavedCustomer()))
    val untaxedCheckout: Checkout = Checkout(oneGiftCertificate, untaxedZip, Some(newSavedCustomer()))
    val checkoutWithoutZip: Checkout = Checkout(oneGiftCertificate, None, Some(newSavedCustomer()))
    val checkoutWithoutCustomer: Checkout = Checkout(oneGiftCertificate, taxedZip, None)

    // check that checkouts only have one of each summary (e.g. subtotal, total, balance)
    taxedCheckout should notHaveDuplicateSummaries
    untaxedCheckout should notHaveDuplicateSummaries
    checkoutWithoutZip should notHaveDuplicateSummaries
    checkoutWithoutCustomer should notHaveDuplicateSummaries
  }

  it should "add taxes for taxed zipcodes" in {
    val taxedCheckout: Checkout = Checkout(oneGiftCertificate, taxedZip, None)
    val untaxedCheckout: Checkout = Checkout(oneGiftCertificate, untaxedZip, None)

    taxedCheckout.taxes should not be (Nil)
    untaxedCheckout.taxes should be (Nil)  // NOTE: this should change if we have some universal tax of sorts
  }




  "A restored checkout" should "contain same line items as saved checkout" in {
    val saved: Checkout = saveModel(newModel)
    val savedItems: LineItems = saved.lineItems
    val restoredItems: LineItems = restoreModel(saved.id).get.lineItems

    savedItems should beContainedIn (restoredItems, "restored")
    restoredItems should beContainedIn (savedItems, "saved")
  }

  it should "update when transacted with additional types" in {
    val savedRestored: Checkout = restoreModel(saveModel(newModel).id).get
    val transformed: Checkout = transformModel(savedRestored) // adds additional types
    val updated: Checkout = saveModel(transformed)    // updates
    val updatedRestored: Checkout = restoreModel(updated.id).get

    savedRestored.id should be (updated.id)
    updated._entity.updated.getTime should be > (savedRestored._entity.updated.getTime)
    updated._entity.updated.getTime should be (updatedRestored._entity.updated.getTime)
  }

  
  it should "have correct balance" in {
    val fresh: Checkout = newModel
    val freshRestored: Checkout = restoreModel(saveModel(fresh).id).get
    val transformed: Checkout = transformModel(freshRestored)
    val transformedRestored: Checkout = restoreModel(saveModel(transformed).id).get

    fresh.balance should haveAmount (expectedBalanceOf(fresh))
    freshRestored.balance should haveAmount (expectedBalanceOf(freshRestored))
    transformed.balance should haveAmount (expectedBalanceOf(transformed))
    transformedRestored.balance should haveAmount (expectedBalanceOf(transformedRestored))
  }

  it should "require payment for only updates with non-zero balance" in {
    // TODO(CE-16): add a checkout in which the balance is zero from discount use
    val paymentRequired: Checkout = newModel
    def transactFails = paymentRequired.transact(None)

    transactFails match {
      case Right(_) => fail()
      case Left(error: CheckoutFailed) =>
    }
  }

  it should "contain same line items as after most recent update" in {
    val saved: Checkout = saveModel(newModel)
    val updated: Checkout = saveModel(transformModel(saved))
    val restored: Checkout = restoreModel(updated.id).get

    updated.lineItems should beContainedIn (restored.lineItems, "restored")
    restored.lineItems should beContainedIn (updated.lineItems, "updated")
  }

  it should "not do anything on transact without any changes being made" in {
    val restoredUnchanged = restoreModel(saveModel(newModel).id).get
    val transactedWithCash = restoredUnchanged.transact(None)
    val transactedWithoutCash = restoredUnchanged.transact(cashTxnTypeFor(restoredUnchanged))

    transactedWithCash should be (Right(restoredUnchanged))
    transactedWithoutCash should be (Right(restoredUnchanged))

  }







  //
  // Data helpers
  //
  def checkoutServices: CheckoutServices = AppConfig.instance[CheckoutServices]
  def payment: StripeTestPayment = { val pment = AppConfig.instance[StripeTestPayment]; pment.bootstrap(); pment }
  def giftCertificateTypeForFriend = GiftCertificateLineItemType("My friend", BigDecimal(75).toMoney())
  def giftCertificateTypeForShittyFriend = GiftCertificateLineItemType("My shittier friend", BigDecimal(25).toMoney())

  def oneGiftCertificate: LineItemTypes = Seq(giftCertificateTypeForFriend)
  def twoGiftCertificates: LineItemTypes = Seq(giftCertificateTypeForFriend, giftCertificateTypeForShittyFriend)

  val taxedZip = Some("98111") // Washington
  val untaxedZip = Some("12345")  // not Washington

  def token: String = payment.testToken().id
  def zeroDollars: Money = BigDecimal(0).toMoney()

  // NOTE(SER-499): zipcode might benefit from being communicated to taxes and cash txn through checkout context

  // txn type is associated with the checkout's customer's account
  def cashTxnTypeFor(checkout: Checkout) = Some( 
    CashTransactionLineItemType(checkout.accountId, taxedZip, Some(token))
  )

  // txn type associated with new unused account id
  def randomCashTxnType = Some(
    CashTransactionLineItemType(newSavedAccount().id, taxedZip, Some(token))
  )

  def expectedTotalOf(checkout: Checkout) = checkout.lineItems.notOfNatures(Summary, Payment).sumAmounts
  def totalPaymentsOf(checkout: Checkout) = checkout.payments.sumAmounts
  def expectedBalanceOf(checkout: Checkout) = expectedTotalOf(checkout) plus totalPaymentsOf(checkout)


  //
  // Matchers
  //
  def notHaveDuplicateSummaries = Matcher { checkout: Checkout =>
    val numSubtotals = checkout.itemTypes.filter(SubtotalLineItemType eq _).length
    val numTotals = checkout.itemTypes.filter(TotalLineItemType eq _).length

    MatchResult(numSubtotals == 1 && numTotals == 1,
      "%d and %d subtotals and totals found.".format(numSubtotals, numTotals),
      "Only 1 subtotal and subtotal found"
    )
  }
}
