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
  "Checkout" should "[class behavior]" in (pending) //new EgraphsTestApplication {}
//  -verify fields take expected values
//  -verify state is inferred correctly
//  -etc


  "A checkout" should "fail to transact without a customer" in {
    val checkout = Checkout(Seq(giftCertificateTypeForFriend), taxedZip, None)
    lazy val failedTransaction = checkout.transact(cashTxnType)
    failedTransaction should be ('Left)
  }

  it should "have a balance of zero after transaction" in {
    val transacted = saveModel(newModel)
    transacted.total should have ('amount (zeroDollars))
  }

  it should "charge the customer for the total on checkout" in {
    val total = newModel.total
    val transacted = saveModel(newModel)
    val maybeTxnItem = transacted.lineItems.find(_.codeType == CodeType.CashTransaction )
    lazy val txnItem = maybeTxnItem.get

    maybeTxnItem should not be (None)
    txnItem.amount should be (total.amount.negated)
  }


  "A restored checkout" should "contain same line items as saved checkout" in {
    val saved = saveModel(newModel)
    val savedItems = saved.lineItems
    val restoredItems = restoreModel(saved.id).get.lineItems

    savedItems should beContainedIn (restoredItems, "restored")
    restoredItems should beContainedIn (savedItems, "saved")
  }

  it should "update when transacted again" in {

  }






  //
  // Helpers
  //
  def checkoutServices = AppConfig.instance[CheckoutServices]
  def payment = { val pment = AppConfig.instance[StripeTestPayment]; pment.bootstrap(); pment }
  def giftCertificateTypeForFriend = GiftCertificateLineItemType("My friend", BigDecimal(75).toMoney())
  def giftCertificateTypeForShittyFriend = GiftCertificateLineItemType("My shittier friend", BigDecimal(25).toMoney())

  val taxedZip = "98111" // Washington
  val untaxedZip = "12345"  // not Washington

  def cardTokenId = ""
  def zeroDollars = BigDecimal(0).toMoney()

  // NOTE(SER-499): zipcode might benefit from being communicated to taxes and cash txn through checkout context

  def cashTxnType = CashTransactionLineItemType(customer.account.id, Some(taxedZip), Some(payment.testToken().id))
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
