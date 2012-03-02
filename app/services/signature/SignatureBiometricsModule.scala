package services.signature

import uk.me.lings.scalaguice.ScalaModule
import services.Utils
import com.google.inject.{Inject, Provider, AbstractModule}

object SignatureBiometricsModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[SignatureBiometricService].toProvider[SignatureBiometricsProvider]
  }
}

private[signature] class SignatureBiometricsProvider @Inject()(yesmaamImpl: Provider[YesMaamSignatureBiometricService],
                                                               testlabImpl: Provider[TestlabXyzmoSignatureBiometricService],
                                                               xyzmoprodImpl: Provider[XyzmoSignatureBiometricService],
                                                               utils: Utils)
  extends Provider[SignatureBiometricService] {

  override def get(): SignatureBiometricService = {
    utils.requiredConfigurationProperty("signature.vendor") match {
      case "yesmaam" =>
        yesmaamImpl.get()

      case "testlab" =>
        testlabImpl.get()

      case "xyzmoprod" =>
        xyzmoprodImpl.get()

      case erroneousValue =>
        throw new IllegalArgumentException(
          "Unrecognized value for 'signature.vendor' in application.conf: " + erroneousValue
        )
    }
  }
}
