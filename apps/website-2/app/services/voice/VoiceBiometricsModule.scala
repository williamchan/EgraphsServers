package services.voice

import uk.me.lings.scalaguice.ScalaModule
import services.Utils
import com.google.inject.{Inject, Provider, AbstractModule}
import services.inject.InjectionProvider
import services.config.ConfigFileProxy

object VoiceBiometricsModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[VoiceBiometricService].toProvider[VoiceBiometricsProvider]
  }
}

private[voice] class VoiceBiometricsProvider @Inject()(yesmaamImpl: Provider[YesMaamVoiceBiometricService],
                                                       fsdevImpl: Provider[VBGDevFSVoiceBiometricService],
                                                       fsprodImpl: Provider[VBGProdFSVoiceBiometricService],
                                                       fsbetaImpl: Provider[VBGBetaFSVoiceBiometricService],
                                                       config: ConfigFileProxy)
  extends InjectionProvider[VoiceBiometricService] {

  override def get(): VoiceBiometricService = {
    config.voiceVendor match {
      case "yesmaam" =>
        yesmaamImpl.get()

      case "fsdev" =>
        fsdevImpl.get()

      case "fsprod" =>
        fsprodImpl.get()

      case "fsbeta" =>
        fsbetaImpl.get()

      case erroneousValue =>
        throw new IllegalArgumentException(
          "Unrecognized value for 'voice.vendor' in application.conf: " + erroneousValue
        )
    }
  }
}
