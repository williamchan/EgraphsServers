package controllers.website

import play.mvc.Controller

import models._
import frontend.storefront.OrderCompleteViewModel
import services.http.ControllerMethod
import play.mvc.results.Redirect
import controllers.WebsiteControllers
import java.util
import services.mvc.ImplicitHeaderAndFooterData

/**
 * Serves the page confirming that an Egraph order was made
 */

private[controllers] trait GetOrderConfirmationEndpoint extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod

  def getOrderConfirmation(orderId: Long) = controllerMethod() {
    // Get order ID from flash scope -- it's OK to just read it
    // because it can only have been provided by our own code (in this case
    // probably PostBuyProductEndpoint)
    import services.http.SafePlayParams.Conversions._

    val maybeOrderIdFromFlash = flash.getLongOption("orderId")

    val maybeHtml = for (
      flashOrderId <- maybeOrderIdFromFlash if flashOrderId == orderId;
      order <- orderStore.findById(flashOrderId)
    ) yield {
      val product = order.product
      val buyer = order.buyer
      val buyerAccount = buyer.account
      val recipient = order.recipient
      val recipientAccount = recipient.account
      val celeb = product.celebrity

      views.frontend.html.celebrity_storefront_complete(
        OrderCompleteViewModel (
          orderDate = order.created,
          orderNumber = order.id,
          buyerName = buyer.name,
          buyerEmail = buyerAccount.email,
          ownerName = recipient.name,
          ownerEmail = recipientAccount.email,
          celebName = celeb.publicName.getOrElse("Anonymous"),
          productName = product.name,
          totalPrice = order.amountPaid,
          guaranteedDeliveryDate = order.inventoryBatch.getExpectedDate
        )
      )
    }

    maybeHtml.getOrElse(new Redirect(reverse(WebsiteControllers.getRootConsumerEndpoint).url))
  }
}

