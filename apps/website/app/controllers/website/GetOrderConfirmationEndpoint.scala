package controllers.website

import play.mvc.Controller

import models._
import services.http.ControllerMethod

/**
 * Serves the page confirming that an Egraph order was made
 */

private[controllers] trait GetOrderConfirmationEndpoint { this: Controller =>
  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod

  def getOrderConfirmation = controllerMethod() {
    // Get order ID from flash scope -- it's OK to just read it
    // because it can only have been provided by our own code (in this case
    // probably PostBuyProductEndpoint)
    val orderIdOption = Option(flash.get("orderId"))

    orderIdOption match {
      case None =>
        Forbidden("I'm sorry, Dave, I'm afraid I can't serve this page for you.")

      case Some(orderId) =>
        orderStore.findById(orderId.toLong) match {
          case None =>
            NotFound("No order with id " + orderId + " exists.")

          case Some(order) =>
            val product = order.product
            val buyer = order.buyer

            views.Application.html.order_confirmation(
              buyer,
              buyer.account,
              order.recipient,
              product.celebrity,
              product,
              order
            )
        }
    }
  }
}

