package services.email

import models.frontend.email._
import models.enums.EmailType
import models.Order
import models.{Celebrity, CelebrityStore}
import services.mail.TransactionalMail
import services.logging.Logging
import services.AppConfig
import services.mail.MailUtils
import services.ConsumerApplication

case class ViewEgraphEmail(
  order: Order,
  mailService: TransactionalMail = AppConfig.instance[TransactionalMail],
  celebrityStore: CelebrityStore = AppConfig.instance[CelebrityStore],
  consumerApp: ConsumerApplication = AppConfig.instance[ConsumerApplication]
) {
  
  import ViewEgraphEmail.log

  def send() = {
    val (emailStack, viewEgraphEmailStack, maybeGiftEmailStack) = prepareViewEgraphEmail

    if (order.buyerId == order.recipientId) {
      log("Sending view egraph mail to: " + order.buyer.account.email)
      mailService.send(emailStack, MailUtils.getViewEgraphTemplateContentParts(
          EmailType.ViewEgraph, viewEgraphEmailStack))
    } else {
      log("Sending view gift egraph mail to: " + order.recipient.account.email)
      mailService.send(emailStack, MailUtils.getViewGiftReceivedEgraphTemplateContentParts(
          EmailType.ViewEgraph, viewEgraphEmailStack))

      log("Sending gift given mail to: " + order.buyer.account.email)
      mailService.send(maybeGiftEmailStack.get, MailUtils.getViewGiftGivenEgraphTemplateContentParts(
          EmailType.ViewEgraph, viewEgraphEmailStack))
    }    
  }
  
  // This function provides a hook for testing the email
  def prepareViewEgraphEmail
  : (EmailViewModel, ViewEgraphEmailViewModel, Option[EmailViewModel])  =
  {
    val (viewEgraphUrl, celebrity, emailStack, maybeGiftEmailStack) = prepareViewEgraphEmailHelper

    val viewEgraphEmailStack = if (order.buyerId == order.recipientId) {
      RegularViewEgraphEmailViewModel(viewEgraphUrl, celebrity.publicName, order.recipientName)
    } else {
      GiftViewEgraphEmailViewModel(viewEgraphUrl, celebrity.publicName, order.recipientName, order.buyer.name)
    }

    (emailStack, viewEgraphEmailStack, maybeGiftEmailStack)
  }
  
  private def prepareViewEgraphEmailHelper: (String, Celebrity, EmailViewModel, Option[EmailViewModel]) = {
    val maybeCelebrity = celebrityStore.findByOrderId(order.id)
    maybeCelebrity match {
      case None => {
        play.Logger.error("There is no celebrity associated with order ID: " + order.id)
        throw new IllegalStateException("There is no celebrity associated with order ID: " + order.id)
      }
      case Some(celebrity) => {
        val buyingCustomer = order.buyer
        val receivingCustomer = order.recipient

        val emailStack = EmailViewModel(
          subject = "I just finished creating an egraph for you",
          fromEmail = celebrity.urlSlug + "@egraphs.com",
          fromName = celebrity.publicName,
          toAddresses = List((receivingCustomer.account.email, None))
        )

        val maybeGiftEmailStack: Option[EmailViewModel] =
          if (buyingCustomer != receivingCustomer) {
            Some(EmailViewModel(
              subject = "Your gift egraph has been delivered",
              fromEmail = "webserver@egraphs.com",
              fromName = "Egraphs",
              toAddresses = List((buyingCustomer.account.email, None))
            ))
          } else None

        val viewEgraphUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)
        (viewEgraphUrl, celebrity, emailStack, maybeGiftEmailStack)
      }
    }
  }  
}

object ViewEgraphEmail extends Logging