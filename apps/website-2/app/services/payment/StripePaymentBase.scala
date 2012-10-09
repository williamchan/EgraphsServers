package services.payment

import com.stripe
import org.joda.money.Money
import scala.collection.JavaConversions._
import services.Utils
import services.config.ConfigFileProxy

abstract class StripePaymentBase extends Payment {
  //
  // Abstract members
  //
  protected def config: ConfigFileProxy
  
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
    stripe.Stripe.apiKey = config.stripeKeySecret
  }

  override val browserModule: String = {
    "stripe-payment"
  }

  override val publishableKey: String = {
    config.stripeKeyPublishable
  }
}

case class StripeCharge(stripeCharge: stripe.model.Charge) extends Charge {
  override val id = stripeCharge.getId
  override val refunded = stripeCharge.getRefunded.booleanValue()
}

case class StripeToken(stripeToken: stripe.model.Token) extends CardToken {
  override val id = stripeToken.getId
}

