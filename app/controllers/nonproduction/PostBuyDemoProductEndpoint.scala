package controllers.nonproduction


import controllers.browser.PostBuyProductEndpoint
import services.Payment

/**
 * For demo purposes only automatically uses test stripe APIs to pay for
 * an order specified only in domain relevant terms (recipient, buyer, etc)
 */
trait PostBuyDemoProductEndpoint { this: PostBuyProductEndpoint =>
  protected def payment: Payment

  def postBuyDemoProduct(recipientName: String,
                         recipientEmail: String,
                         buyerName: String,
                         buyerEmail: String,
                         desiredText: Option[String],
                         personalNote: Option[String]) = {
    this.postBuyProduct(
      recipientName,
      recipientEmail,
      buyerName,
      buyerEmail,
      payment.fakeStripeToken.getId,
      desiredText,
      personalNote
    )
  }
}
