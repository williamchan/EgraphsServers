package services.voice

import uk.me.lings.scalaguice.ScalaModule
import services.Utils
import com.google.inject.{Inject, Provider, AbstractModule}

object VoiceBiometricsModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[VoiceBiometricService].toProvider[VoiceBiometricsProvider]
  }
}

private[voice] class VoiceBiometricsProvider @Inject()(yesmaamImpl: Provider[YesMaamVoiceBiometricService],
                                                       fsdevImpl: Provider[VBGDevFSVoiceBiometricService],
                                                       fsprodImpl: Provider[VBGProdFSVoiceBiometricService],
                                                       fsbetaImpl: Provider[VBGBetaFSVoiceBiometricService],
                                                       utils: Utils)
  extends Provider[VoiceBiometricService] {

  override def get(): VoiceBiometricService = {
    utils.requiredConfigurationProperty("voice.vendor") match {
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
