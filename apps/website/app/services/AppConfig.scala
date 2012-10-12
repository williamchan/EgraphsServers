package services

import blobs.BlobModule
import cache.CacheModule
import db.DBModule
import graphics.GraphicsModule
import http.HttpModule
import mail.{BulkMail, BulkMailProvider, MailProvider, TransactionalMail}
import models._
import filters.FilterServices
import models.vbg._
import models.xyzmo._
import mvc.MvcModule
import payment.PaymentModule
import signature.SignatureBiometricsModule
import social.SocialModule
import voice.VoiceBiometricsModule
import uk.me.lings.scalaguice.{InjectorExtensions, ScalaModule}
import com.google.inject.{Injector, Singleton, Guice, AbstractModule}

class AppConfig extends AbstractModule with ScalaModule {
  override def configure() {
    // Services
    bind[TransactionalMail].toProvider[MailProvider]
    bind[BulkMail].toProvider[BulkMailProvider]

    install(DBModule)
    install(CacheModule)
    install(HttpModule)
    install(BlobModule)
    install(PaymentModule)
    install(SignatureBiometricsModule)
    install(VoiceBiometricsModule)
    install(GraphicsModule)
    install(SocialModule)
    install(MvcModule)

    // Model services
    bind[AccountServices].in[Singleton]
    bind[AddressServices].in[Singleton]
    bind[BlobKeyServices].in[Singleton]
    bind[CashTransactionServices].in[Singleton]
    bind[CustomerServices].in[Singleton]
    bind[CelebrityServices].in[Singleton]
    bind[AdministratorServices].in[Singleton]
    bind[EgraphServices].in[Singleton]
    bind[EnrollmentBatchServices].in[Singleton]
    bind[EnrollmentSampleServices].in[Singleton]
    bind[FailedPurchaseDataServices].in[Singleton]
    bind[FilterServices].in[Singleton]
    bind[ImageAssetServices].in[Singleton]
    bind[InventoryBatchServices].in[Singleton]
    bind[InventoryBatchProductServices].in[Singleton]
    bind[OrderServices].in[Singleton]
    bind[PrintOrderServices].in[Singleton]
    bind[ProductServices].in[Singleton]
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

/**
 * Accessor to the application's dependency injector.
 */
object AppConfig {

  import InjectorExtensions._
  import uk.me.lings.scalaguice.typeLiteral
  import uk.me.lings.scalaguice.KeyExtensions._

  val injector: Injector = {
    // Put a try-catch on making injector and print the error because Guice
    // exceptions get turned into UnexpectedExceptions by Play, making them
    // almost impossible to catch.
    try {
      Guice.createInjector(new AppConfig)
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

  def instance[T: Manifest] = {
    injector.instance[T]
  }

  /**
   * Gets a class instance bound to a particular annotation. This can be useful for getting, for example,
   * the particular Properties instance which is bound to PlayConfig, or our application's configuration.
   *
   * @tparam A the annotation type (e.g. PlayConfig)
   * @tparam T the instance type (e.g. Properties)
   *
   * @return the instance in question, if it had a matching binding. Otherwise throws
   *     an exception.
   */
  def annotatedInstance[A <: java.lang.annotation.Annotation: Manifest, T: Manifest]: T = {
    injector.getInstance(typeLiteral[T].annotatedWith[A])
  }
}
