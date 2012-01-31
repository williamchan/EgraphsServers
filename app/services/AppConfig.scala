package services

import blobs.{BlobModule, BlobVendor, Blobs}
import uk.me.lings.scalaguice.{InjectorExtensions, ScalaModule}
import services.db.Schema
import com.google.inject.{Provider, Singleton, Guice, AbstractModule}
import models._

class AppConfig extends AbstractModule with ScalaModule {
  override def configure() {
    bind[Schema].in[Singleton]

    // Services
    bind[Mail].toProvider(Mail.MailProvider)
    install(BlobModule)

    // Model services
    bind[AccountServices].in[Singleton]
    bind[CashTransactionServices].in[Singleton]
    bind[CustomerServices].in[Singleton]
    bind[CelebrityServices].in[Singleton]
    bind[OrderServices].in[Singleton]
    bind[ProductServices].in[Singleton]
    bind[EnrollmentBatchServices].in[Singleton]
    bind[EnrollmentSampleServices].in[Singleton]
    bind[SignatureSampleServices].in[Singleton]
    bind[VoiceSampleServices].in[Singleton]
    bind[EgraphServices].in[Singleton]
    bind[ImageAssetServices].in[Singleton]
  }
}

object AppConfig {
  import InjectorExtensions._

  val injector = Guice.createInjector(new AppConfig)

  def instance[T : Manifest] = {
    injector.instance[T]
  }
}