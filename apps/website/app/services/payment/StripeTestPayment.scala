package services.payment

import com.stripe
import com.google.inject.Inject
import services.config.ConfigFileProxy

/**
 * Stripe-based implementation of [[services.Payment]] service using test account
 */
class StripeTestPayment @Inject()(protected val config: ConfigFileProxy) extends StripePaymentBase {

  /**
   * Returns a fake stripe token
   */
  override def testToken(): CardToken = {
    import java.lang.Integer

    val defaultCardParams = new java.util.HashMap[String, Object]()
    val defaultChargeParams = new java.util.HashMap[String, Object]()

    defaultCardParams.put("number", "4242424242424242")
    defaultCardParams.put("exp_month", new Integer(12))
    defaultCardParams.put("exp_year", new Integer(2015))
    defaultCardParams.put("cvc", "123")
    defaultCardParams.put("name", "Java Bindings Cardholder")
    defaultCardParams.put("address_line1", "522 Ramona St")
    defaultCardParams.put("address_line2", "Palo Alto")
    defaultCardParams.put("address_zip", "94301")
    defaultCardParams.put("address_state", "CA")
    defaultCardParams.put("address_country", "USA")

    defaultChargeParams.put("amount", new Integer(100))
    defaultChargeParams.put("currency", "usd")
    defaultChargeParams.put("card", defaultCardParams)

    StripeToken(stripe.model.Token.create(defaultChargeParams))
  }

  /**
   * @return whether this implementation is for testing purposes
   */
  def isTest = true
}
