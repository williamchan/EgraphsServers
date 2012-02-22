package services.payment

import uk.me.lings.scalaguice.ScalaModule
import services.inject.ClosureProviders
import services.Utils
import com.google.inject.{Inject, Provider, AbstractModule}

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
  niceImpl: Provider[NicePayment]
) extends Provider[Payment]
{
  override def get(): Payment = {
    Utils.requiredConfigurationProperty("payment.vendor") match {
      case "nice" =>
        niceImpl.get()

      case "stripe" =>
        stripeImpl.get()

      case erroneousValue =>
        throw new IllegalArgumentException(
          "Unrecognized value for 'payment.vendor' in application.conf: " + erroneousValue
        )
    }
  }
}

