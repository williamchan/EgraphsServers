package services.payment

import com.stripe
import org.joda.money.Money
import scala.collection.JavaConversions._
import services.Utils

/**
 * Stripe-based implementation of [[services.Payment]] service
 */
class StripePayment extends Payment {
  //
  // Payment Methods
  //
  override def charge(amount: Money, cardTokenId: String, description: String=""): Charge = {
    val chargeMap = Map[String, AnyRef](
      "amount" -> amount.getAmountMinor,
      "currency" -> amount.getCurrencyUnit.getCode.toLowerCase,
      "card" -> cardTokenId,
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

  override def bootstrap {
    stripe.Stripe.apiKey = Utils.requiredConfigurationProperty("stripe.key.secret")
  }

  override val browserModule: String = {
    "stripe-payment"
  }

  override val publishableKey: String = {
    Utils.requiredConfigurationProperty("stripe.key.publishable")
  }
}

case class StripeCharge (stripeCharge: stripe.model.Charge) extends Charge {
  override val id = stripeCharge.getId
}

case class StripeToken (stripeToken: stripe.model.Token) extends CardToken {
  override val id = stripeToken.getId
}

