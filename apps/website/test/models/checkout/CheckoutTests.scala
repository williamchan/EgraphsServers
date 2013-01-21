package models.checkout

import utils._
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.Finance.TypeConversions._
import models.checkout.checkout._
import org.scalatest.matchers.{MatchResult, Matcher}
import services.db.{Schema, TransactionSerializable, DBSession}
import models.enums.CodeType
import services.payment.StripeTestPayment
import TestData._
import models.checkout.Checkout.{CheckoutFailed, FailedCheckoutWithCharge}


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
  override def newModel: Checkout = Checkout(Seq(giftCertificateTypeForFriend), taxedZip, Some(newSavedCustomer()))
  override def saveModel(toSave: Checkout): Checkout = toSave.transact(cashTxnTypeFor(toSave)) match {
    case Right(checkout) => checkout
    case Left(failure) => fail("failed with " + failure)
  }
  override def restoreModel(id: Long): Option[Checkout] = checkoutServices.findById(id)

  override def transformModel(toTransform: Checkout) = {
    toTransform.withAdditionalTypes(Seq(giftCertificateTypeForShittyFriend))
  }



  //
  // Companion test cases
  //
  "Checkout" should "add summaries to new checkouts" in {
    val taxedCheckout = Checkout(oneGiftCertificate, taxedZip, Some(newSavedCustomer()))
    val untaxedCheckout = Checkout(oneGiftCertificate, untaxedZip, Some(newSavedCustomer()))
    val checkoutWithoutZip = Checkout(oneGiftCertificate, "", Some(newSavedCustomer()))
    val checkoutWithoutCustomer = Checkout(oneGiftCertificate, taxedZip, None)

    taxedCheckout should haveCorrectSummaries
    untaxedCheckout should haveCorrectSummaries
    checkoutWithoutZip should haveCorrectSummaries
    checkoutWithoutCustomer should haveCorrectSummaries
  }


  it should "add summaries to restored checkouts" in (pending)

  // TODO(SER-499): test expected values from checkout with added types (balance, total, etc)
  // need to re-evaluate total and balance
      //total updated, not the balance
        //  +: expected behavior, "total" always available
        //  -: balance may need to be a line item (instead of just Money)

      //total constant, not updated
        //  +: easier adding of types, less micromanaging items
        //  -: sum additional items for balance

      //total as balance
        //  +: no extra balance concept
        //  -: unexpected, actual total is calculation


  it should "add taxes to checkouts in taxed zipcodes" in {
    val checkout = Checkout(oneGiftCertificate, taxedZip, None)
    checkout.taxes should not be (Nil)
  }


  //
  // Checkout test cases
  //
  "A checkout" should "fail to transact without a customer" in {
    val checkout = Checkout(oneGiftCertificate, taxedZip, None)
    lazy val failedTransaction = checkout.transact(randomCashTxnType)

    failedTransaction should be ('left)

    failedTransaction match {
      // TODO(SER-499): check that maybeCharge is refunded
      case Left(error: FailedCheckoutWithCharge) =>
        error.maybeCharge should be ('defined)
        error.maybeCharge.get.refunded should be (true)
      case Left(other: CheckoutFailed) => fail("Expected error with charge, received " + other.getClass)
    }
  }

  it should "have a balance of zero after transaction" in {
    val transacted = saveModel(newModel)
    transacted.total should have ('amount (zeroDollars))
    transacted.balance should be (zeroDollars)
  }

  it should "charge the customer for the total on checkout" in {
    val total = newModel.total
    val transacted = saveModel(newModel)
    val txnItem = transacted.lineItems.find(_.codeType == CodeType.CashTransaction ).get

    txnItem.amount should be (total.amount.negated)
  }


  "A restored checkout" should "contain same line items as saved checkout" in {
    val saved = saveModel(newModel)
    val savedItems = saved.lineItems
    val restoredItems = restoreModel(saved.id).get.lineItems

    savedItems should beContainedIn (restoredItems, "restored")
    restoredItems should beContainedIn (savedItems, "saved")
  }

  it should "update when transacted after being persisted in initial transaction" in {
    val saved = saveModel(newModel)
    val transformed = transformModel(saved)
    val updated = saveModel(transformed)
    val restored = restoreModel(updated.id).get

    saved.id should be (updated.id)
    updated._entity.updated.getTime should be > (saved._entity.updated.getTime)
    updated._entity.updated.getTime should equal (restored._entity.updated.getTime)
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

  def haveCorrectSummaries = Matcher { checkout: Checkout =>
    val numSubtotals = checkout.lineItemTypes.filter(SubtotalLineItemType eq _).length
    val numTotals = checkout.lineItemTypes.filter(TotalLineItemType eq _).length

    MatchResult(numSubtotals == 1 && numTotals == 1,
      "%d and %d subtotals and totals found.".format(numSubtotals, numTotals),
      "Only 1 subtotal and subtotal found"
    )
  }
}
