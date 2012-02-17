package services.payment

import org.joda.money.Money
import services.Utils

/**
 * Service for charging credit cards based on a Stripe-like token system.
 */
trait Payment {
  /**
   * Creates a payment charge equivalent to the provided cash against the card token.
   *
   * @param amount the amount of cash to charge
   *
   * @param cardTokenId the ID of the token produced by the javascript payment API. For testing
   *   you can also get one from [[services.Payment.testToken]]
   *
   * @param description description of the transaction for easy viewing on the Stripe console.
   */
  def charge(amount: Money, cardTokenId: String, description: String): Charge

  /**
   * Creates a test version of a chargeable card token
   */
  def testToken: CardToken

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
}

/**
 * A token that represents an opportunity to charge a credit/debit card
 */
trait CardToken {
  /** Globally unique ID of the card token */
  def id: String
}

