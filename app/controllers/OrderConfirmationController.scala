package controllers

import play.mvc.Controller

import models._

/**
 * Serves the page confirming that an eGraph order was made
 */
object OrderConfirmationController extends Controller with DBTransaction
{
  
  def confirm = {
    // Get order ID from flash scope -- it's OK to just read it
    // because it can only have been provided by our own code (in this case
    // probably CelebrityProductController.buy)
    val orderIdOption = Option(flash.get("orderId"))

    orderIdOption match {
      case None =>
        Forbidden("I'm sorry, Dave, I'm afraid I can't serve this page for you.")

      case Some(orderId) =>
        Order.findById(orderId.toLong) match {
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