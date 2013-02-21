package models.checkout

import org.joda.money.Money
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.{MatchResult, Matcher}
import org.junit.runner.RunWith
import play.api.libs.json.JsNull
import utils._
import utils.TestData._
import services.AppConfig
import services.Finance.TypeConversions._
import models.checkout.LineItemMatchers._
import models.checkout.checkout.Conversions._
import models.checkout.Checkout._
import models.enums.LineItemNature._
import LineItemTestData._

@RunWith(classOf[JUnitRunner])
class CheckoutTests extends EgraphsUnitTest
  with CheckoutTestCases
  with DateShouldMatchers
  with DBTransactionPerTest
  with CanInsertAndUpdateEntityWithLongKeyTests[Checkout, CheckoutEntity]
{
  import CheckoutScenario.RichCheckoutConversions._

  //
  // CanInsertAndUpdateEntityTests members
  //
  override def newModel(): FreshCheckout = newCheckout
  override def saveModel(toSave: Checkout): Checkout = toSave.transact(Some(randomCashTransactionType)) match {
    case Right(checkout: Checkout) => checkout
    case Left(failure) => fail("saveModel failed with " + failure)
  }
  override def restoreModel(id: Long): Option[Checkout] = Checkout.restore(id)
  override def transformModel(toTransform: Checkout) = {
    toTransform.withAdditionalTypes(Seq(giftCertificateTypeForLesserFriend))
  }


  //
  // CheckoutTestCases members
  //
  override def scenarios = Seq(
    CheckoutScenario(oneGiftCertificate, twoGiftCertificates),
    CheckoutScenario(twoGiftCertificates, Nil)
  )

  //
  // Checkout Tests
  //
  "A checkout" should "add taxes for taxed zipcodes" in eachScenario { implicit scenario =>
    val untaxedCheckout = initialCheckout.withZipcode(untaxedZip)
    val taxedCheckout = initialCheckout.withZipcode(taxedZip)
    val untaxedRestored = untaxedCheckout.restored
    val taxedRestored = taxedCheckout.restored

    untaxedCheckout.taxes should be (Nil)
    untaxedRestored.taxes should be (Nil)
    taxedCheckout.taxes should not be (Nil)
    taxedRestored.taxes should not be (Nil)
  }

  it should "not have duplicate summaries" in eachScenario { implicit scenario =>
    val taxedCheckout = initialCheckout.withZipcode(taxedZip)
    val taxedRestored = taxedCheckout.restored
    val untaxedCheckout = initialCheckout.withZipcode(untaxedZip)
    val untaxedRestored = untaxedCheckout.restored
    val checkoutWithoutZip = initialCheckout.withZipcode(None)
    val withoutZipRestored = checkoutWithoutZip.restored

    // check that checkouts only have one of each summary (e.g. subtotal, total, balance)
    taxedCheckout should notHaveDuplicateSummaries
    taxedRestored should notHaveDuplicateSummaries
    untaxedCheckout should notHaveDuplicateSummaries
    untaxedRestored should notHaveDuplicateSummaries
    checkoutWithoutZip should notHaveDuplicateSummaries
    withoutZipRestored should notHaveDuplicateSummaries
  }

  it should "be previewable without a buyer Account" in {
    val checkout = Checkout.create(twoGiftCertificates, None, None)
    checkout.toJson should not be (JsNull)
  }

  "Checkout#toJson" should "match the api endpoint spec" in (pending)

  //
  // Helpers
  //
  def giftCertificateTypeForFriend = GiftCertificateLineItemType("My friend", BigDecimal(75).toMoney())
  def giftCertificateTypeForLesserFriend = GiftCertificateLineItemType("My lesser friend", BigDecimal(25).toMoney())

  def oneGiftCertificate: LineItemTypes = Seq(giftCertificateTypeForFriend)
  def twoGiftCertificates: LineItemTypes = Seq(giftCertificateTypeForFriend, giftCertificateTypeForLesserFriend)

  val taxedZip = Some("98111") // Washington
  val untaxedZip = Some("12345")  // not Washington

  def checkoutServices: CheckoutServices = AppConfig.instance[CheckoutServices]

  //
  // Checkout Matchers
  //
  def notHaveDuplicateSummaries = Matcher { checkout: Checkout =>
    val numSubtotals = checkout.itemTypes.filter(SubtotalLineItemType eq _).size
    val numTotals = checkout.itemTypes.filter(TotalLineItemType eq _).size

    MatchResult(numSubtotals == 1 && numTotals == 1,
      "Only 1 subtotal and subtotal found",
      "%d and %d subtotals and totals found.".format(numSubtotals, numTotals)
    )
  }
}
