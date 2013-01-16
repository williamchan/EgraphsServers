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
  override def newModel: Checkout = Checkout(Seq(giftCertificateTypeForFriend), taxedZip, Some(customer))
  override def saveModel(toSave: Checkout): Checkout = toSave.transact(cashTxnType) match {
    case Right(checkout) => checkout
    case Left(failure) => fail("failed with " + failure)
  }
  override def restoreModel(id: Long): Option[Checkout] = checkoutServices.findById(id)

  override def transformModel(toTransform: Checkout) = {
    toTransform.withAdditionalTypes(Seq(giftCertificateTypeForShittyFriend))
  }



  //
  // Test cases
  //
  "Checkout" should "add summaries to new checkouts" in {
    val taxedCheckout = Checkout(oneGiftCertificate, taxedZip, Some(customer))
    val untaxedCheckout = Checkout(oneGiftCertificate, untaxedZip, Some(customer))
    val checkoutWithoutZip = Checkout(oneGiftCertificate, "", Some(customer))
    val checkoutWithoutCustomer = Checkout(oneGiftCertificate, taxedZip, None)

    taxedCheckout.lineItemTypes.filter(SubtotalLineItemType eq _) should have length (1)
    taxedCheckout.lineItemTypes.filter(TotalLineItemType eq _) should have length (1)
    taxedCheckout.subtotal should not be (null)
    taxedCheckout.total should not be (null)

    untaxedCheckout.lineItemTypes.filter(SubtotalLineItemType eq _) should have length (1)
    untaxedCheckout.lineItemTypes.filter(TotalLineItemType eq _) should have length (1)
    untaxedCheckout.subtotal should not be (null)
    untaxedCheckout.total should not be (null)

    checkoutWithoutZip.lineItemTypes.filter(SubtotalLineItemType eq _) should have length (1)
    checkoutWithoutZip.lineItemTypes.filter(TotalLineItemType eq _) should have length (1)
    checkoutWithoutZip.subtotal should not be (null)
    checkoutWithoutZip.total should not be (null)

    checkoutWithoutCustomer.lineItemTypes.filter(SubtotalLineItemType eq _) should have length (1)
    checkoutWithoutCustomer.lineItemTypes.filter(TotalLineItemType eq _) should have length (1)
    checkoutWithoutCustomer.subtotal should not be (null)
    checkoutWithoutCustomer.total should not be (null)
  }

  it should "add summaries to restored checkouts" in (pending)

  it should "add taxes to checkouts in taxed zipcodes" in {
    val checkout = Checkout(oneGiftCertificate, taxedZip, None)
    checkout.taxes should not be (Nil)
  }


  "A checkout" should "fail to transact without a customer" in {
    val checkout = Checkout(oneGiftCertificate, taxedZip, None)
    lazy val failedTransaction = checkout.transact(cashTxnType)

    failedTransaction should be ('left)

    failedTransaction match {
      // TODO(SER-499): check that maybeCharge is refunded
      case _ =>
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
  // Helpers
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

  def cashTxnType = CashTransactionLineItemType(customer.account.id, Some(taxedZip), Some(token))
  def customer = TestData.newSavedCustomer()

  def beContainedIn(otherItems: LineItems, checkoutState: String = "other") = Matcher { left: LineItems =>
    val notInOtherItems = left.filterNot { item => otherItems.exists(item equalsLineItem _) }
    val successMessage = "All items were contained in %s checkout".format(checkoutState)
    val failMessage = "Line items with id's (%s) were not contained in %s checkout".format(
      notInOtherItems.map(item => item.id).mkString(", "),
      checkoutState
    )

    MatchResult(notInOtherItems.isEmpty, failMessage, successMessage)
  }
}
