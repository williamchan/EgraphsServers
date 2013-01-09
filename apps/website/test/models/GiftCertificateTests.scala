package models

import utils._
import services.Finance.TypeConversions._
import services.AppConfig


class GiftCertificateTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesTests[GiftCertificate, GiftCertificateEntity, Long]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def store = AppConfig.instance[GiftCertificateStore]

  override def newIdValue: Long = 0L
  override def improbableIdValue: Long = java.lang.Integer.MAX_VALUE
  override def newModel: GiftCertificate = GiftCertificate("Joe Schmoe", BigDecimal(50).toMoney())
  override def saveModel(toSave: GiftCertificate): GiftCertificate = toSave.save()
  override def restoreModel(id: Long): Option[GiftCertificate] = store.findById(id)
  override def transformModel(toTransform: GiftCertificate): GiftCertificate = {
    val coupon = toTransform.coupon.get
    toTransform.coupon.set(coupon use BigDecimal(25).toMoney())
  }


  /**
   * TODO(SER-471): additional test cases --
   * -invalid amount fails
   * -get gifter name, email
   * -store methods
   * -saveWithLineItem
   *
   * -create gift certificate and then make purchase with the coupon it creates :O
   */
}
