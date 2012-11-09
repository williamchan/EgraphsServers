package models

import com.google.inject.{Singleton, AbstractModule}
import models.categories.CategoryServices
import vbg._
import xyzmo._
import uk.me.lings.scalaguice.ScalaModule

/**
 * Guice bindings for all model services.
 */
object ModelModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[AccountServices].in[Singleton]
    bind[AddressServices].in[Singleton]
    bind[AdministratorServices].in[Singleton]    
    bind[BlobKeyServices].in[Singleton]
    bind[CashTransactionServices].in[Singleton]
    bind[CategoryServices].in[Singleton]
    bind[CelebrityServices].in[Singleton]    
    bind[CustomerServices].in[Singleton]
    bind[EgraphServices].in[Singleton]
    bind[EnrollmentBatchServices].in[Singleton]
    bind[EnrollmentSampleServices].in[Singleton]
    bind[FailedPurchaseDataServices].in[Singleton]
    bind[ImageAssetServices].in[Singleton]
    bind[InventoryBatchServices].in[Singleton]
    bind[InventoryBatchProductServices].in[Singleton]
    bind[OrderServices].in[Singleton]
    bind[PrintOrderServices].in[Singleton]
    bind[ProductServices].in[Singleton]
    bind[VideoAssetCelebrityServices].in[Singleton]
    bind[VideoAssetServices].in[Singleton]
    bind[VBGStartEnrollmentServices].in[Singleton]
    bind[VBGAudioCheckServices].in[Singleton]
    bind[VBGEnrollUserServices].in[Singleton]
    bind[VBGFinishEnrollTransactionServices].in[Singleton]
    bind[VBGStartVerificationServices].in[Singleton]
    bind[VBGVerifySampleServices].in[Singleton]
    bind[VBGFinishVerifyTransactionServices].in[Singleton]
    bind[XyzmoAddUserServices].in[Singleton]
    bind[XyzmoDeleteUserServices].in[Singleton]
    bind[XyzmoAddProfileServices].in[Singleton]
    bind[XyzmoEnrollDynamicProfileServices].in[Singleton]
    bind[XyzmoVerifyUserServices].in[Singleton]
  }
}