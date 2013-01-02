package models

import utils._
import services.Finance.TypeConversions._


class GiftCertificateTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with CanInsertAndUpdateAsThroughServicesTests[GiftCertificate, GiftCertificateEntity, Long]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  override def newIdValue: Long = 0L
  override def improbableIdValue: Long = java.lang.Integer.MAX_VALUE
  override def newModel: GiftCertificate = GiftCertificate("Joe Schmoe", BigDecimal(50).toMoney())
  override def saveModel(toSave: GiftCertificate): GiftCertificate = toSave.save()
  override def restoreModel(id: Long): Option[GiftCertificate] = None
  override def transformModel(toTransform: GiftCertificate): GiftCertificate = {
    val coupon = toTransform.coupon.get
    toTransform.coupon.set(coupon use BigDecimal(25).toMoney())
  }

}
