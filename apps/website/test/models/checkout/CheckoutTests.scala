package models.checkout

import utils._
import org.joda.money.{CurrencyUnit, Money}
import org.scalatest.matchers.{MatchResult, Matcher}
import services.AppConfig
import services.db.{Schema, TransactionSerializable, DBSession}
import services.Finance.TypeConversions._
import services.payment.StripeTestPayment
import models.checkout.checkout._
import models.checkout.Checkout._
import TestData._


class CheckoutTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesTests[Checkout, CheckoutEntity, Long]
  // TODO(SER-499): create equivalent of CreatedUpdatedEntityTests[Long, CheckoutEntity] for CanInsertAndUpdate...
  with DateShouldMatchers
  with DBTransactionPerTest
{
  //
  // CanInsertAndUpdateAsThroughServicesTests members
  //
  override def newIdValue = 0
  override def improbableIdValue = java.lang.Integer.MAX_VALUE
  override def newModel: Checkout = Checkout(Seq(giftCertificateTypeForFriend), Some(taxedZip), Some(newSavedCustomer()))
  override def saveModel(toSave: Checkout): Checkout = toSave.transact(cashTxnTypeFor(toSave)) match {
    case Right(checkout) => checkout
    case Left(failure) => fail("failed with " + failure)
  }
  override def restoreModel(id: Long): Option[Checkout] = checkoutServices.findById(id)

  override def transformModel(toTransform: Checkout) = {
    toTransform.withAdditionalTypes(Seq(giftCertificateTypeForShittyFriend))
  }




  //
  // Companion Object test cases
  //
  "Checkout" should "not add duplicate summaries" in {
    val taxedCheckout = Checkout(oneGiftCertificate, Some(taxedZip), Some(newSavedCustomer()))
    val untaxedCheckout = Checkout(oneGiftCertificate, Some(untaxedZip), Some(newSavedCustomer()))
    val checkoutWithoutZip = Checkout(oneGiftCertificate, None, Some(newSavedCustomer()))
    val checkoutWithoutCustomer = Checkout(oneGiftCertificate, Some(taxedZip), None)

    // check that checkouts only have one of each summary (e.g. subtotal, total, balance)
    taxedCheckout should notHaveDuplicateSummaries
    untaxedCheckout should notHaveDuplicateSummaries
    checkoutWithoutZip should notHaveDuplicateSummaries
    checkoutWithoutCustomer should notHaveDuplicateSummaries
  }

  it should "add taxes to checkouts in taxed zipcodes" in {
    val checkout = Checkout(oneGiftCertificate, Some(taxedZip), None)
    checkout.taxes should not be (Nil)
  }


  //
  // Checkout test cases
  //

  // TODO(SER-499): additional save test cases
  // -don't make zero-value charges

  "A checkout" should "fail to transact without a customer" in {
    val checkout = Checkout(oneGiftCertificate, Some(taxedZip), None)
    lazy val failedTransaction = checkout.transact(randomCashTxnType)

    failedTransaction should be ('left)

    failedTransaction match {
      case Left(error: FailedCheckoutWithCharge) =>
        error.maybeCharge should be ('defined)
        error.maybeCharge.get.refunded should be (true)
      case Left(other: CheckoutFailed) => fail("Expected error with charge, received " + other.getClass)
    }
  }

  it should "have a balance of zero after transaction" in {
    val transacted = saveModel(newModel)
    transacted.total should haveNegatedAmountOf (transacted.payments.head)
    transacted.balance should haveAmount (zeroDollars)
  }

  it should "charge the customer for the total on checkout" in {
    val total = newModel.total
    val transacted = saveModel(newModel)
    val txnItem = transacted.payments.head

    txnItem should haveNegatedAmountOf (total)
  }




  // TODO(SER-499): additional test cases
  // -require payment
  // -make payment appropriately
  // -do nothing without add. types

  "A restored checkout" should "contain same line items as saved checkout" in {
    val saved = saveModel(newModel)
    val savedItems = saved.lineItems
    val restoredItems = restoreModel(saved.id).get.lineItems

    savedItems should beContainedIn (restoredItems, "restored")
    restoredItems should beContainedIn (savedItems, "saved")
  }

  it should "update when transacted with additional types" in {
    val savedRestored = restoreModel(saveModel(newModel).id).get
    val transformed = transformModel(savedRestored) // adds additional types
    val updated = saveModel(transformed)    // updates
    val updatedRestored = restoreModel(updated.id).get

    savedRestored.id should be (updated.id)
    updated._entity.updated.getTime should be > (savedRestored._entity.updated.getTime)
    updated._entity.updated.getTime should be (updatedRestored._entity.updated.getTime)
  }

  it should "have correct balance" in {
    val restored = restoreModel(saveModel(newModel).id)
  }

  it should "require payment for update with non-zero balance" in {

  }

  it should "contain same line items as after most recent update" in {
    val saved = saveModel(newModel)
    val updated = saveModel(transformModel(saved))
    val restored = restoreModel(updated.id).get

    updated.lineItems should beContainedIn (restored.lineItems, "restored")
    restored.lineItems should beContainedIn (updated.lineItems, "updated")
  }




  //
  // Data helpers
  //
  def checkoutServices = AppConfig.instance[CheckoutServices]
  def payment = { val pment = AppConfig.instance[StripeTestPayment]; pment.bootstrap(); pment }
  def giftCertificateTypeForFriend = GiftCertificateLineItemType("My friend", BigDecimal(75).toMoney())
  def giftCertificateTypeForShittyFriend = GiftCertificateLineItemType("My shittier friend", BigDecimal(25).toMoney())

  def oneGiftCertificate = Seq(giftCertificateTypeForFriend)
  def twoGiftCertificates = Seq(giftCertificateTypeForFriend, giftCertificateTypeForShittyFriend)

  val taxedZip = "98111" // Washington
  val untaxedZip = "12345"  // not Washington

  def token = payment.testToken().id
  def zeroDollars = BigDecimal(0).toMoney()

  // NOTE(SER-499): zipcode might benefit from being communicated to taxes and cash txn through checkout context

  // txn type is associated with the checkout's customer's account
  def cashTxnTypeFor(checkout: Checkout) = CashTransactionLineItemType(checkout.account.id, Some(taxedZip), Some(token))

  // txn type associated with new unused account id
  def randomCashTxnType = CashTransactionLineItemType(newSavedAccount().id, Some(taxedZip), Some(token))








  //
  // Matchers
  //
  def beContainedIn(otherItems: LineItems, checkoutState: String = "other") = Matcher { left: LineItems =>
    val notInOtherItems = left.filterNot { item => otherItems.exists(item equalsLineItem _) }
    val successMessage = "All items were contained in %s checkout".format(checkoutState)
    val failMessage = "Line items with id's (%s) were not contained in %s checkout".format(
      notInOtherItems.map(item => item.id).mkString(", "),
      checkoutState
    )

    MatchResult(notInOtherItems.isEmpty, failMessage, successMessage)
  }

  def notHaveDuplicateSummaries = Matcher { checkout: Checkout =>
    val numSubtotals = checkout.lineItemTypes.filter(SubtotalLineItemType eq _).length
    val numTotals = checkout.lineItemTypes.filter(TotalLineItemType eq _).length

    MatchResult(numSubtotals == 1 && numTotals == 1,
      "%d and %d subtotals and totals found.".format(numSubtotals, numTotals),
      "Only 1 subtotal and subtotal found"
    )
  }

  def haveAmount(desiredAmount: Money) = Matcher { left: LineItem[_] =>
    MatchResult(left.amount == desiredAmount,
      (left.amount + " did not equal " + desiredAmount),
      "LineItem has desired amount"
    )
  }

  def haveAmountOf(right: LineItem[_]) = haveAmount(right.amount)
  def haveNegatedAmountOf(right: LineItem[_]) = haveAmount(right.amount.negated)
}
