package services

import org.joda.money.Money
import scala.collection.JavaConversions._
import com.stripe

trait Payment {
  /**
   * Creates a payment charge equivalent to the provided cash against the card token.
   *
   * @param amount the amount of cash to charge
   *
   * @param cardToken the ID of the token produced by the javascript payment API. For testing
   *   you can also get one from [[services.Payment.testToken]]
   *
   * @param description description of the transaction for easy viewing on the Stripe console.
   */
  def charge(amount: Money,  cardToken: String, description: String): Charge

  /**
   * Creates a test version of a chargeable card token
   */
  def testToken: CardToken

  /**
   * Prepares the payment system for use at application start
   */
  def bootstrap()
}

trait Charge {
  /** Unique ID of the charge */
  def id: String
}


trait CardToken {
  /** Globally unique ID of the card token */
  def id: String
}


//
// Stripe Implementation
//
case class StripeCharge (stripeCharge: stripe.model.Charge) extends Charge {
  override val id = stripeCharge.getId
}

case class StripeToken (stripeToken: stripe.model.Token) extends CardToken {
  override val id = stripeToken.getId
}

/**
 * Helper for charging and sending cash in and out of the system.
 */
class StripePayment extends Payment {
  //
  // Payment Methods
  //
  override def charge(amount: Money, cardToken: String, description: String=""): Charge = {
    val chargeMap = Map[String, AnyRef](
      "amount" -> amount.getAmountMinor,
      "currency" -> amount.getCurrencyUnit.getCode.toLowerCase,
      "card" -> cardToken,
      "description" -> description
    )

    StripeCharge(stripe.model.Charge.create(chargeMap))
  }

  /**
   * Returns a fake stripe token
   */
  override def testToken: CardToken = {
    // TODO: Delete this method and any referencing code before launch. This is purely
    // for spring training demonstrations.
    import java.lang.Integer

    val defaultCardParams = new java.util.HashMap[String, Object]();
    val defaultChargeParams = new java.util.HashMap[String, Object]();

    defaultCardParams.put("number", "4242424242424242");
    defaultCardParams.put("exp_month", new Integer(12));
    defaultCardParams.put("exp_year", new Integer(2015))
    defaultCardParams.put("cvc", "123");
    defaultCardParams.put("name", "Java Bindings Cardholder");
    defaultCardParams.put("address_line1", "522 Ramona St");
    defaultCardParams.put("address_line2", "Palo Alto");
    defaultCardParams.put("address_zip", "94301");
    defaultCardParams.put("address_state", "CA");
    defaultCardParams.put("address_country", "USA");

    defaultChargeParams.put("amount", new Integer(100));
    defaultChargeParams.put("currency", "usd");
    defaultChargeParams.put("card", defaultCardParams);

    StripeToken(stripe.model.Token.create(defaultChargeParams))
  }
  
  override def bootstrap = {
    stripe.Stripe.apiKey = Utils.requiredConfigurationProperty("stripe.key.secret")
  }
}

object Payment {
  /** Keys for using Stripe either in test or production mode */
  object StripeKey {
    def publishable: String = {
      Utils.requiredConfigurationProperty("stripe.key.publishable")
    }
  }
}

