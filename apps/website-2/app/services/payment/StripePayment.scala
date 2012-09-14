package services.payment

/**
 * Stripe-based implementation of [[services.Payment]] service using live account
 */
class StripePayment extends StripePaymentBase {

  override def testToken(): CardToken = {
    throw new UnsupportedOperationException("Live Stripe implementation does not support test token")
  }

  /**
   * @return whether this implementation is for testing purposes
   */
  def isTest = false
}
