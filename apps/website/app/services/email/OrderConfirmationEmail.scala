package services.email

import org.apache.commons.mail.HtmlEmail
import services.mail.TransactionalMail
import services.logging.Logging
import models.frontend.email.OrderConfirmationEmailViewModel

case class OrderConfirmationEmail(
  orderConfirmationEmailStack: OrderConfirmationEmailViewModel,
  mailService: TransactionalMail
) {

  import OrderConfirmationEmail.log
  
  def send() {
    val mail = new HtmlEmail()
    mail.setFrom("webserver@egraphs.com", "Egraphs")
    mail.addTo(orderConfirmationEmailStack.buyerEmail, orderConfirmationEmailStack.buyerName)
    mail.setSubject("Order Confirmation")

    val htmlMsg = views.html.frontend.email.order_confirmation(orderConfirmationEmailStack)
    val textMsg = views.txt.frontend.email.order_confirmation(orderConfirmationEmailStack).toString()
    
    log("Sending order confirmation mail to : " + orderConfirmationEmailStack.buyerName + " for order ID " + orderConfirmationEmailStack.orderId)
    mailService.send(mail, Some(textMsg), Some(htmlMsg))
  }
}

object OrderConfirmationEmail extends Logging