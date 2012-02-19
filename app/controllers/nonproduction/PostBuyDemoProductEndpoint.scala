package controllers.nonproduction

import services.payment.Payment
import controllers.website.PostBuyProductEndpoint

trait PostBuyDemoProductEndpoint { this: PostBuyProductEndpoint =>
  protected def payment: Payment

  /**
   * For demo purposes only automatically uses test stripe APIs to pay for
   * an order specified only in domain relevant terms (recipient, buyer, etc)
   */
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
      payment.testToken.id,
      desiredText,
      personalNote
    )
  }
}
