package libs

import org.joda.money.Money
import com.stripe.model.Charge
import scala.collection.JavaConversions._

/**
 * Helper for charging and sending cash in and out of the system.
 */
object Payment {
  /**
   * Creates a skype charge equivalent tot he provided cash against the Stripe card token.
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

