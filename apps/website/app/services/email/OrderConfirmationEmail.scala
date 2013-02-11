package services.email

import org.apache.commons.mail.HtmlEmail
import services.mail.TransactionalMail
import services.logging.Logging
import models.frontend.email._
import services.mail.MailUtils
import models.enums.EmailType

case class OrderConfirmationEmail(
  orderConfirmationEmailStack: OrderConfirmationEmailViewModel,
  mailService: TransactionalMail
) {

  import OrderConfirmationEmail.log
  
  def send() {
    val emailStack = EmailViewModel(subject = "Order Confirmation",
                                    fromEmail = "webserver@egraphs.com",
                                    fromName = "Egraphs",
                                    toAddresses = List((orderConfirmationEmailStack.buyerEmail,
                                        Some(orderConfirmationEmailStack.buyerName))))

    val orderConfirmationTemplateContentParts = MailUtils.getOrderConfirmationTemplateContentParts(EmailType.OrderConfirmation, orderConfirmationEmailStack)
    log("Sending order confirmation mail to : " + orderConfirmationEmailStack.buyerName + " for order ID " + orderConfirmationEmailStack.orderId)

    mailService.send(emailStack, orderConfirmationTemplateContentParts)
  }
}

object OrderConfirmationEmail extends Logging