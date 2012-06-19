package services.payment

import com.stripe
import org.joda.money.Money
import scala.collection.JavaConversions._
import services.Utils

abstract class StripePaymentBase extends Payment {
  //
  // Payment Methods
  //
  override def charge(amount: Money, cardTokenId: String, description: String = ""): Charge = {
    val chargeMap = Map[String, AnyRef](
      "amount" -> amount.getAmountMinor,
      "currency" -> amount.getCurrencyUnit.getCode.toLowerCase,
      "card" -> cardTokenId,
      "description" -> description
    )

    StripeCharge(stripe.model.Charge.create(chargeMap))
  }

  override def refund(chargeId: String): Charge = {
    val chargeToRefund = stripe.model.Charge.retrieve(chargeId)
    StripeCharge(chargeToRefund.refund())
  }

  override def bootstrap() {
    stripe.Stripe.apiKey = Utils.requiredConfigurationProperty("stripe.key.secret")
  }

  override val browserModule: String = {
    "stripe-payment"
  }

  override val publishableKey: String = {
    Utils.requiredConfigurationProperty("stripe.key.publishable")
  }
}

case class StripeCharge(stripeCharge: stripe.model.Charge) extends Charge {
  override val id = stripeCharge.getId
  override val refunded = stripeCharge.getRefunded.booleanValue()
}

case class StripeToken(stripeToken: stripe.model.Token) extends CardToken {
  override val id = stripeToken.getId
}

