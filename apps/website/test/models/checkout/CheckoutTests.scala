package models.checkout

import utils._
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import services.Finance.TypeConversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


class CheckoutTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesTests[Checkout, CheckoutEntity, Long]
  with DateShouldMatchers
  with DBTransactionPerTest
{

  //
  // Helpers
  //
  def checkoutServices = AppConfig.instance[CheckoutServices]
  def giftCertificateTypeForFriend = GiftCertificateLineItemType("My friend", BigDecimal(75).toMoney())
  def giftCertificateTypeForShittyFriend = GiftCertificateLineItemType("My shittier friend", BigDecimal(25).toMoney())

  val taxedZip = "98111" // Washington
  val untaxedZip = "12345"  // not Washington


  //
  // CanInsertAndUpdateAsThroughServicesTests members
  //
  override def newIdValue = 0
  override def improbableIdValue = java.lang.Integer.MAX_VALUE
  override def newModel: Checkout = Checkout(Seq(giftCertificateTypeForFriend), taxedZip)
  override def saveModel(toSave: Checkout): Checkout = toSave.transact()
  override def restoreModel(id: Long): Option[Checkout] = checkoutServices.findById(id)

  override def transformModel(toTransform: Checkout) = {
    toTransform.withAdditionalTypes(Seq(giftCertificateTypeForShittyFriend))
  }



  //
  // Test cases
  //
  "Checkout" should "[class behavior]" in (pending) //new EgraphsTestApplication {}

  "A checkout" should "[object behavior]" in (pending)





}
