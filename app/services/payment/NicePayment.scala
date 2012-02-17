package services.payment

import org.joda.money.Money

/**
 * "Nice" payment service implementation that always says that the payment
 * was successful, regardless of inputs or outputs.
 */
class NicePayment extends Payment {
  override def charge(amount: Money, cardTokenId: String, description: String): Charge = {
    NiceCharge
  }

  override def testToken: CardToken = {
    NiceToken
  }

  override def bootstrap() { }
}

object NiceCharge extends Charge {
  override val id = {
    "This was a test charge against services.payment.NicePayment. Have a great day!"
  }
}

object NiceToken extends CardToken {
  override val id = {
    "This was a test token from services.payment.NicePayment. Have a great day!"
  }
}