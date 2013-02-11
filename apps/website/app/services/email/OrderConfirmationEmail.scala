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
    //TODO: figure out how to give the name too (probably already mentioned this elsewhere)
    //mail.addTo(orderConfirmationEmailStack.buyerEmail, orderConfirmationEmailStack.buyerName)
    
    val emailStack = EmailViewModel(subject = "Order Confirmation",
                                    fromEmail = "webserver@egraphs.com",
                                    fromName = "Egraphs",
                                    toEmail = orderConfirmationEmailStack.buyerEmail)

    val orderConfirmationTemplateContentParts = MailUtils.getOrderConfirmationTemplateContentParts(EmailType.OrderConfirmation, orderConfirmationEmailStack)
    log("Sending order confirmation mail to : " + orderConfirmationEmailStack.buyerName + " for order ID " + orderConfirmationEmailStack.orderId)

    mailService.send(emailStack, orderConfirmationTemplateContentParts)
  }
}

object OrderConfirmationEmail extends Logging