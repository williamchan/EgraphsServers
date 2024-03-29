package services.signature

import net.codingwell.scalaguice.ScalaModule
import services.Utils
import com.google.inject.{Inject, Provider, AbstractModule, Singleton}
import services.inject.InjectionProvider
import services.config.ConfigFileProxy

object SignatureBiometricsModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[SignatureBiometricService].toProvider[SignatureBiometricsProvider].in[Singleton]
  }
}

private[signature] class SignatureBiometricsProvider @Inject()(yesmaamImpl: Provider[YesMaamSignatureBiometricService],
                                                               xyzmoprodImpl: Provider[XyzmoProdSignatureBiometricService],
                                                               xyzmobetaImpl: Provider[XyzmoBetaSignatureBiometricService],
                                                               config: ConfigFileProxy)
  extends InjectionProvider[SignatureBiometricService] {

  override def get(): SignatureBiometricService = {
    config.signatureVendor match {
      case "yesmaam" =>
        yesmaamImpl.get()

      case "xyzmoprod" =>
        xyzmoprodImpl.get()

      case "xyzmobeta" =>
        xyzmobetaImpl.get()

      case erroneousValue =>
        throw new IllegalArgumentException(
          "Unrecognized value for 'signature.vendor' in application.conf: " + erroneousValue
        )
    }
  }
}
