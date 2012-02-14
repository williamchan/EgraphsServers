package services

import org.joda.money.Money
import scala.collection.JavaConversions._
import com.stripe.model.{Token, Charge}

/**
 * Helper for charging and sending cash in and out of the system.
 */
class Payment {
  /**
   * Creates a Stripe charge equivalent tot he provided cash against the Stripe card token.
   *
   * @param amount the amount of cash to charge
   *
   * @param cardToken the ID of the token produced by the javascript Stripe API. For testing
   *   you can also get one from [[utils.TestData.newStripeToken]]
   *
   * @param description description of the transaction for easy viewing on the Stripe console.
   */
  def charge(amount: Money, cardToken: String, description: String=""): Charge = {
    val chargeMap = Map[String, AnyRef](
      "amount" -> amount.getAmountMinor,
      "currency" -> amount.getCurrencyUnit.getCode.toLowerCase,
      "card" -> cardToken,
      "description" -> description
    )

    Charge.create(chargeMap)
  }

  /**
   * Returns a fake stripe token
   */
  def fakeStripeToken: Token = {
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

    Token.create(defaultChargeParams)
  }

}

object Payment {
  /** Keys for using Stripe either in test or production mode */
  object StripeKey {
    def secret: String = {
      Utils.requiredConfigurationProperty("stripe.key.secret")
    }

    def publishable: String = {
      Utils.requiredConfigurationProperty("stripe.key.publishable")
    }
  }
}

