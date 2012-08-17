package controllers.website

import play.mvc.Controller

import models._
import frontend.storefront.OrderCompleteViewModel
import services.http.ControllerMethod
import services.mvc.{OrderCompleteViewModelFactory, ImplicitHeaderAndFooterData}
import play.templates.Html

/**
 * Serves the page confirming that an Egraph order was made
 */

private[controllers] trait GetOrderConfirmationEndpoint extends ImplicitHeaderAndFooterData
{ this: Controller =>

  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod
  protected def orderCompleteViewModelFactory: OrderCompleteViewModelFactory

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
      views.frontend.html.celebrity_storefront_complete(
        orderCompleteViewModelFactory.fromOrder(order)
      )
    }

    maybeHtml.getOrElse(NotFound("Order confirmation has expired."))
  }
}