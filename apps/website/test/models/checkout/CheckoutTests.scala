package models.checkout

import utils._
import org.joda.money.{CurrencyUnit, Money}
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CheckoutTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesTests[Checkout, CheckoutEntity]
//  with CreatedUpdatedEntityTests[Long, CheckoutEntity]
  with DateShouldMatchers
  with DBTransactionPerTest
{


  //
  // Helpers
  //
  val taxedZip = "98111" // Washington
  val untaxedZip = "12345"  // not Washington

  implicit def numberToDollars(amount: Double) = new { def dollars = Money.of(CurrencyUnit.USD, amount) }

  def checkoutStore = AppConfig.instance[CheckoutStore]

  def giftCertificateTypeForFriend = GiftCertificateLineItemType("My friend", 75 dollars)
  def giftCertificateTypeForShittyFriend = GiftCertificateLineItemType("My shittier friend", 25 dollars)



  //
  // CreatedUpdatedEntityTests members
  //
  //  override def newEntity: CheckoutEntity = new CheckoutEntity()
  //  override def saveEntity(toSave: CheckoutEntity): CheckoutEntity = toSave.insert()
  //  override def restoreEntity(id: Long): Option[CheckoutEntity] = None
  //  override def transformEntity(toTransform: CheckoutEntity): CheckoutEntity = {
  //    toTransform
  //  }

  //
  // CanInsertAndUpdateAsThroughServicesTests members
  //
  override def newIdValue = 0
  override def improbableIdValue = java.lang.Integer.MAX_VALUE
  override def newModel: Checkout = Checkout(Seq(giftCertificateTypeForFriend), taxedZip)
  override def saveModel(toSave: Checkout): Checkout = toSave //.insert()
  override def restoreModel(id: Long): Option[Checkout] = checkoutStore.findById(id)
  override def transformModel(toTransform: Checkout) = {
    toTransform //.addAdditionalTypes(Seq(giftCertificateTypeForShittyFriend))
  }



  //
  // Test cases
  //
  "Checkout" should "[class behavior]" in (pending) //new EgraphsTestApplication {}

  "A checkout" should "[object behavior]" in (pending)





}
