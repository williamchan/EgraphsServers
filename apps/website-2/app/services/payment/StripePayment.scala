package services.payment

import com.google.inject.Inject
import services.config.ConfigFileProxy

/**
 * Stripe-based implementation of [[services.Payment]] service using live account
 */
class StripePayment @Inject()(protected val config: ConfigFileProxy) extends StripePaymentBase {

  override def testToken(): CardToken = {
    throw new UnsupportedOperationException("Live Stripe implementation does not support test token")
  }

  /**
   * @return whether this implementation is for testing purposes
   */
  def isTest = false
}
