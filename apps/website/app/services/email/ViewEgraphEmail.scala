package services.email

import services.mail.TransactionalMail
import services.logging.Logging
import models.frontend.email._
import services.mail.MailUtils
import models.enums.EmailType
import services.ConsumerApplication
import models.Order
import models.Celebrity

case class ViewEgraphEmail(
  order: Order
) {
  
  import ViewEgraphEmail.log

  def send() = {
    val (emailStack, viewEgraphEmailStack) = prepareViewEgraphEmail

    if (order.buyerId == order.recipientId) {
      log("Sending view egraph mail to : " + order.buyer.account.email)
      order.services.mail.send(emailStack, MailUtils.getViewEgraphTemplateContentParts(
          EmailType.ViewEgraph, viewEgraphEmailStack))
    } else {
      log("Sending view gift egraph mail to : " + order.recipient.account.email)
      order.services.mail.send(emailStack, MailUtils.getViewGiftEgraphTemplateContentParts(
          EmailType.ViewEgraph, viewEgraphEmailStack))
    }    
  }
  
  // This function provides a hook for testing the email
  def prepareViewEgraphEmail
  : (EmailViewModel, ViewEgraphEmailViewModel)  =
  {
    val (viewEgraphUrl, celebrity, emailStack) = prepareViewEgraphEmailHelper

    val viewEgraphEmailStack = if (order.buyerId == order.recipientId) {
      RegularViewEgraphEmailViewModel(viewEgraphUrl, celebrity.publicName, order.recipientName)
    } else {
      GiftViewEgraphEmailViewModel(viewEgraphUrl, celebrity.publicName, order.recipientName, order.buyer.name)
    }

    (emailStack, viewEgraphEmailStack)
  }
  
  private def prepareViewEgraphEmailHelper: (String, Celebrity, EmailViewModel) = {
    val maybeCelebrity = order.services.celebrityStore.findByOrderId(order.id)
    maybeCelebrity match {
      case None => {
        play.Logger.error("There is no celebrity associated with order ID: " + order.id)
        throw new IllegalStateException("There is no celebrity associated with order ID: " + order.id)
      }
      case Some(celebrity) => {
        val buyingCustomer = order.buyer
        val receivingCustomer = order.recipient

        val toAddresses = List((receivingCustomer.account.email, None))
        if (buyingCustomer != receivingCustomer) {
          // TODO: change this to a CC address when Mandrill allows it
          toAddresses ::: List(buyingCustomer.account.email, None)
        }

        val emailStack = EmailViewModel(subject = "I just finished signing your Egraph",
                                        fromEmail = celebrity.urlSlug + "@egraphs.com",
                                        fromName = celebrity.publicName,
                                        toAddresses = toAddresses)

        val viewEgraphUrl = order.services.consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)
        (viewEgraphUrl, celebrity, emailStack)
      }
    }
  }  
}

object ViewEgraphEmail extends Logging