package services

import blobs.BlobModule
import db.DBModule
import http.PlayConfig
import mail.{MailProvider, Mail}
import models._
import models.vbg._
import models.xyzmo._
import payment.PaymentModule
import signature.SignatureBiometricsModule
import voice.VoiceBiometricsModule
import uk.me.lings.scalaguice.{InjectorExtensions, ScalaModule}
import java.util.Properties
import play.Play
import com.google.inject.{Injector, Singleton, Guice, AbstractModule}

class AppConfig extends AbstractModule with ScalaModule {
  override def configure() {
    // Play helpers
    bind[Properties].annotatedWith[PlayConfig].toInstance(Play.configuration)

    // Services
    bind[Mail].toProvider[MailProvider]
    install(DBModule)
    install(BlobModule)
    install(PaymentModule)
    install(SignatureBiometricsModule)
    install(VoiceBiometricsModule)

    // Model services
    bind[AccountServices].in[Singleton]
    bind[CashTransactionServices].in[Singleton]
    bind[CustomerServices].in[Singleton]
    bind[CelebrityServices].in[Singleton]
    bind[AdministratorServices].in[Singleton]
    bind[EgraphServices].in[Singleton]
    bind[EnrollmentBatchServices].in[Singleton]
    bind[EnrollmentSampleServices].in[Singleton]
    bind[ImageAssetServices].in[Singleton]
    bind[OrderServices].in[Singleton]
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

object AppConfig {

  import InjectorExtensions._

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
}