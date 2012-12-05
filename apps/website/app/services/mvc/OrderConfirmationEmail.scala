package services.mvc

import java.util.Date
import org.joda.money.Money
import java.text.SimpleDateFormat
import org.apache.commons.mail.{Email, HtmlEmail}
import play.api.mvc.RequestHeader
import services.mail.TransactionalMail
import services.logging.Logging

case class OrderConfirmationEmail(
  buyerName: String,
  buyerEmail: String,
  recipientName: String,
  recipientEmail: String,
  celebrityName: String,
  productName: String,
  orderId: Long,
  orderCreated: Date,
  expectedDate: Date,
  cost: Money,
  faqHowLongLink: String,
  hasPrintOrder: Boolean,
  mailService: TransactionalMail
) {
  import OrderConfirmationEmail.log
  
  private val dateFormat = new SimpleDateFormat("MMMM dd, yyyy")
  import services.Finance.TypeConversions._

  def send() {
    val mail = new HtmlEmail()
    mail.setFrom("webserver@egraphs.com", "Egraphs")
    mail.addTo(buyerEmail, buyerName)
    mail.setSubject("Order Confirmation")

    val htmlMsg = views.html.frontend.email_order_confirmation(
      buyerName = buyerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      celebrityName = celebrityName,
      productName = productName,
      orderDate = dateFormat.format(orderCreated),
      orderId = orderId.toString,
      pricePaid = cost.formatSimply,
      deliveredByDate = dateFormat.format(expectedDate), // all new Orders have expectedDate... will turn this into Date instead of Option[Date]
      faqHowLongLink = faqHowLongLink,
      hasPrintOrder = hasPrintOrder
    )
    val textMsg = views.html.frontend.email_order_confirmation_text(
      buyerName = buyerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      celebrityName = celebrityName,
      productName = productName,
      orderDate = dateFormat.format(orderCreated),
      orderId = orderId.toString,
      pricePaid = cost.formatSimply,
      deliveredByDate = dateFormat.format(expectedDate), // all new Orders have expectedDate... will turn this into Date instead of Option[Date]
      faqHowLongLink = faqHowLongLink,
      hasPrintOrder = hasPrintOrder
    ).toString()
    
    log("Sending order confirmation mail to : " + buyerName + " for order ID " + orderId)
    mailService.send(mail, Some(textMsg), Some(htmlMsg))
  }
}

object OrderConfirmationEmail extends Logging