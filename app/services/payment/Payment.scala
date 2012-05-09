package services.payment

import org.joda.money.Money

/**
 * Service for charging credit cards based on a Stripe-like token system.
 */
trait Payment {
  /**
   * Creates a payment charge equivalent to the provided cash against the card token.
   *
   * @param amount the amount of cash to charge
   * @param cardTokenId the ID of the token produced by the javascript payment API. For testing
   *   you can also get one from [[services.Payment.testToken]]. Once a cardTokenId exists, then no
   *   instances of com.stripe.exception.CardException have been thrown. That means that the card should be valid.
   * @param description description of the transaction for easy viewing on the Stripe console.
   * @throws com.stripe.exception.InvalidRequestException if multiple attempts to charge a cardTokenId occur
   */
  def charge(amount: Money, cardTokenId: String, description: String): Charge


  /**
   * Refunds the charge in full.
   *
   * @param chargeId the Stripe id of the charge to refund.
   * @throws com.stripe.exception.InvalidRequestException if charge with chargeId does not exist or has already been refunded
   */
  def refund(chargeId: String): Charge

  /**
   * Creates a test version of a chargeable card token
   */
  def testToken(): CardToken

  /**
   * Prepares the payment system for use at application start
   */
  def bootstrap()

  /**
   * Provides the require.js specifier for the browser module that corresponds
   * to this payment implementation
   */
  def browserModule: String

  /**
   * Public key for the browser module to generate tokens for consumption by the
   * server.
   */
  def publishableKey: String
}

/**
 * A single charge of money to a credit card
 */
trait Charge {
  /** Unique ID of the charge */
  def id: String

  /** Whether a charge has been refunded */
  def refunded: Boolean
}

/**
 * A token that represents an opportunity to charge a credit/debit card
 */
trait CardToken {
  /** Globally unique ID of the card token */
  def id: String
}

