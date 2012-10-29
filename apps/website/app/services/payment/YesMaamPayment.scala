package services.payment

import org.joda.money.Money

/**
 * "Nice" payment service implementation that always says that the payment
 * was successful, regardless of inputs or outputs.
 */
class YesMaamPayment extends Payment {
  override def charge(amount: Money, cardTokenId: String, description: String): Charge = {
    NiceCharge
  }

  def refund(chargeId: String) = {
    NiceRefundedCharge
  }

  override def testToken(): CardToken = {
    NiceToken
  }

  def isTest = true

  override def bootstrap() { }

  override val browserModule: String = {
    "yes-maam-payment"
  }

  override val publishableKey: String = {
    "No publishable key -- anything is groovy for services.payment.YesMaamPayment"
  }
}

object NiceCharge extends Charge {
  override val id = {
    "This was a test charge against services.payment.YesMaamPayment. Have a great day!"
  }
  override val refunded = false
}

object NiceRefundedCharge extends Charge {
  override val id = {"This was a test charge against services.payment.YesMaamPayment. Have a great day!"}
  override val refunded = true
}

object NiceToken extends CardToken {
  override val id = {
    "This was a test token from services.payment.YesMaamPayment. Have a great day!"
  }
}