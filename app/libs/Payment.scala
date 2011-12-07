package libs

import org.joda.money.Money
import com.stripe.model.Charge
import scala.collection.JavaConversions._

object Payment {
  def charge(amount: Money, cardToken: String, description: String=""): Charge = {
    val chargeMap = Map[String, AnyRef](
      "amount" -> amount.getAmountMinor,
      "currency" -> amount.getCurrencyUnit.getCode.toLowerCase,
      "card" -> cardToken,
      "description" -> description
    )

    Charge.create(chargeMap)
  }

  //
  // Private members
  //
  object StripeKey {
    def secret: String = {
      Utils.requiredConfigurationProperty("stripe.key.secret")
    }

    def publishable: String = {
      Utils.requiredConfigurationProperty("stripe.key.publishable")
    }
  }
}