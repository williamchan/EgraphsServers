package services.payment

import uk.me.lings.scalaguice.ScalaModule
import services.Utils
import com.google.inject.{Inject, Provider, AbstractModule}
import services.inject.InjectionProvider
import services.config.ConfigFileProxy

/**
 * Payment service configuration
 */
object PaymentModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[Payment].toProvider[PaymentProvider]
  }
}

private[payment] class PaymentProvider @Inject()(
  stripeImpl: Provider[StripePayment],
  stripeTestImpl: Provider[StripeTestPayment],
  yesmaamImpl: Provider[YesMaamPayment],
  config: ConfigFileProxy
) extends InjectionProvider[Payment]
{
  override def get(): Payment = {
    config.paymentVendor match {
      case "yesmaam" =>
        yesmaamImpl.get()

      case "stripetest" =>
        stripeTestImpl.get()

      case "stripe" =>
        stripeImpl.get()

      case erroneousValue =>
        throw new IllegalArgumentException(
          "Unrecognized value for 'payment.vendor' in application.conf: " + erroneousValue
        )
    }
  }
}

