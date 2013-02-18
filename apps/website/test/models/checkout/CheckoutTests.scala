package models.checkout

import LineItemTestData._
import models.checkout.checkout.Conversions._
import services.AppConfig
import services.Finance.TypeConversions._
import utils._
import org.scalatest.matchers.{MatchResult, Matcher}


class CheckoutTests extends EgraphsUnitTest
  with CheckoutTestCases
  with DateShouldMatchers
  with DBTransactionPerTest
  with CanInsertAndUpdateEntityWithLongKeyTests[Checkout, CheckoutEntity]
//  with HasTransientServicesTests[Checkout]
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
  // HasTransientServicesTests members
  //
//  override def assertModelsEqual(a: Checkout, b: Checkout) {
//    import LineItemMatchers._
//    a.lineItems should haveLineItemEqualityTo(b.lineItems)
//    a._entity should be (b._entity)
//  }

  "A cachable checkout" should "be cachable" in (pending)

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
  def notHaveDuplicateSummaries: Matcher[Checkout] = Matcher { checkout: Checkout =>
    val numSubtotals = checkout.itemTypes.filter(SubtotalLineItemType eq _).size
    val numTotals = checkout.itemTypes.filter(TotalLineItemType eq _).size

    MatchResult(numSubtotals == 1 && numTotals == 1,
      "Only 1 subtotal and subtotal found",
      "%d and %d subtotals and totals found.".format(numSubtotals, numTotals)
    )
  }
}
