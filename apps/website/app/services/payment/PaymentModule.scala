package services.payment

import uk.me.lings.scalaguice.ScalaModule
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
  stripeTestImpl: Provider[StripeTestPayment],
  yesmaamImpl: Provider[YesMaamPayment],
  utils: Utils
) extends Provider[Payment]
{
  override def get(): Payment = {
    utils.requiredConfigurationProperty("payment.vendor") match {
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
