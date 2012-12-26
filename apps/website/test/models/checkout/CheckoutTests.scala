package models.checkout

import utils._
import org.joda.money.{CurrencyUnit, Money}

class CheckoutTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesTests[Checkout, CheckoutEntity]
  //with CreatedUpdatedEntityTests[Long, CheckoutEntity]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  /*override def newEntity: CheckoutEntity = new CheckoutEntity()
  override def saveEntity(toSave: CheckoutEntity): CheckoutEntity = toSave.insert()
  override def restoreEntity(id: Long): Option[CheckoutEntity] = None
  override def transformEntity(toTransform: CheckoutEntity): CheckoutEntity = {
    toTransform
  }*/


  val taxedZip = "12345"  // not Washington
  val untaxedZip = "98111" // Washington

  override def newIdValue = 0
  override def improbableIdValue = Integer.MAX_VALUE
  override def newModel: Checkout = Checkout(taxedZip, Seq(GiftCertificateLineItemType("My friend", dollars(75))))
  override def saveModel(toSave: Checkout): Checkout = toSave.insert()
  override def restoreModel(id: Long): Option[Checkout] = None
  override def transformModel(toTransform: Checkout) = toTransform.add(Seq(GiftCertificateLineItemType("My shittier friend", dollars(50))))

  protected def dollars(amount: Double) = Money.of(CurrencyUnit.USD, amount)




  "Checkout" should "[class behavior]" in (pending)
  "A checkout" should "[object behavior]" in (pending)
}
