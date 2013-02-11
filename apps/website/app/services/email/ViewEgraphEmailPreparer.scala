package services.email

import models.Celebrity
import models.Coupon
import models.Order
import models.OrderServices
import models.frontend.email.ViewEgraphEmailViewModel
import org.apache.commons.mail.{ Email, HtmlEmail }
import play.api.templates.Html
import controllers.website.GetEgraphEndpoint
import services.coupon.CouponCreator
import utils.TestData
import models.frontend.email.EmailViewModel

/**
 * A home for utility functions that help prepare the "egraph signed" emails.
 * Useful for both the regular case and the gift-giving case.
 */
object ViewEgraphEmailPreparer {

  def prepareViewEgraphEmailHelper(order: Order, services: OrderServices): (String, Celebrity, EmailViewModel) = {

    val maybeCelebrity = services.celebrityStore.findByOrderId(order.id)
    maybeCelebrity match {
      case None => {
        play.Logger.error("There is no celebrity associated with order ID: " + order.id)
        throw new IllegalStateException("There is no celebrity associated with order ID: " + order.id)
      }
      case Some(celebrity) => {

        // TODO: figure out how to add this to the json header
        //email.addReplyTo("webserver@egraphs.com")

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

        val viewEgraphUrl = services.consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)
        (viewEgraphUrl, celebrity, emailStack)
      }
    }
  }
}