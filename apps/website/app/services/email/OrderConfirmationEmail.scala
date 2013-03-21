package services.email

import models.frontend.email._
import models.enums.EmailType
import services.mail.TransactionalMail
import services.logging.Logging
import services.AppConfig
import services.mail.MailUtils

case class OrderConfirmationEmail(
  orderConfirmationEmailStack: OrderConfirmationEmailViewModel,
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail]
) {

  import OrderConfirmationEmail.log
  
  def send() {
    val emailStack = EmailViewModel(
      subject = "Order Confirmation",
      fromEmail = EmailUtils.generalFromEmail,
      fromName = EmailUtils.generalFromName,
      toAddresses = List((orderConfirmationEmailStack.buyerEmail, Some(orderConfirmationEmailStack.buyerName)))
    )

    val orderConfirmationTemplateContentParts = MailUtils.getOrderConfirmationTemplateContentParts(
      EmailType.OrderConfirmation, orderConfirmationEmailStack)

    log(s"Sending order confirmation mail to: ${orderConfirmationEmailStack.buyerEmail} for order ID: ${orderConfirmationEmailStack.orderId}")
    mailService.send(emailStack, orderConfirmationTemplateContentParts)
  }
}

object OrderConfirmationEmail extends Logging