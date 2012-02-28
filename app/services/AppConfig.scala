package services

import blobs.BlobModule
import com.google.inject.{Singleton, Guice, AbstractModule}
import http.PlayConfig
import models._
//import models.vbg._
import models.xyzmo._
import payment.PaymentModule
import services.db.Schema
import signature.{SignatureBiometricService, XyzmoSignatureBiometricService}
import uk.me.lings.scalaguice.{InjectorExtensions, ScalaModule}
import voice.{VoiceBiometricService, VBGVoiceBiometricService}
import java.util.Properties
import play.Play

class AppConfig extends AbstractModule with ScalaModule {
  override def configure() {
    bind[Schema].in[Singleton]

    // Play helpers
    bind[Properties].annotatedWith[PlayConfig].toInstance(Play.configuration)

    // Services
    bind[Mail].toProvider(Mail.MailProvider)
    bind[SignatureBiometricService].to[XyzmoSignatureBiometricService]
    bind[VoiceBiometricService].to[VBGVoiceBiometricService]

    install(PaymentModule)
    install(BlobModule)

    // Model services
    bind[AccountServices].in[Singleton]
    bind[CashTransactionServices].in[Singleton]
    bind[CustomerServices].in[Singleton]
    bind[CelebrityServices].in[Singleton]
    bind[EgraphServices].in[Singleton]
    bind[EnrollmentBatchServices].in[Singleton]
    bind[EnrollmentSampleServices].in[Singleton]
    bind[ImageAssetServices].in[Singleton]
    bind[OrderServices].in[Singleton]
    bind[ProductServices].in[Singleton]
    bind[SignatureSampleServices].in[Singleton]
    //    bind[VBGStartEnrollmentResponseServices].in[Singleton]
    //    bind[VBGAudioCheckResponseServices].in[Singleton]
    //    bind[VBGEnrollUserResponseServices].in[Singleton]
    //    bind[VBGFinishEnrollTransactionResponseServices].in[Singleton]
    //    bind[VBGStartVerificationResponseServices].in[Singleton]
    //    bind[VBGVerifySampleResponseServices].in[Singleton]
    //    bind[VBGFinishVerifyTransactionResponseServices].in[Singleton]
    bind[VoiceSampleServices].in[Singleton]
    bind[XyzmoAddUserResponseServices].in[Singleton]
    bind[XyzmoDeleteUserResponseServices].in[Singleton]
    bind[XyzmoAddProfileResponseServices].in[Singleton]
    bind[XyzmoEnrollDynamicProfileResponseServices].in[Singleton]
    bind[XyzmoVerifyUserResponseServices].in[Singleton]
  }
}

object AppConfig {
  import InjectorExtensions._

  val injector = Guice.createInjector(new AppConfig)

  def instance[T : Manifest] = {
    injector.instance[T]
  }
}