package controllers.website

import play.api.mvc._
import models._
import checkout.{CheckoutServices, LineItemStore}
import services.http.ControllerMethod
import services.mvc.{OrderCompleteViewModelFactory, ImplicitHeaderAndFooterData}

/**
 * Serves the page confirming to the purchasing customer that an Egraph order was made.
 * See celebrity_storefront_complete.scala.html
 */
private[controllers] trait GetOrderConfirmationEndpoint extends ImplicitHeaderAndFooterData
{ this: Controller =>

  //
  // Services
  //
  protected def orderStore: OrderStore
  protected def lineItemStore: LineItemStore
  protected def checkoutServices: CheckoutServices
  protected def controllerMethod: ControllerMethod
  protected def orderCompleteViewModelFactory: OrderCompleteViewModelFactory

  //
  // Controllers
  //
  def getOrderConfirmation(orderId: Long) = controllerMethod.withForm() { implicit authToken =>
     Action { implicit request =>
      // Get order ID from flash scope -- it's OK to just read it
      // because it can only have been provided by our own code (in this case
      // probably PostBuyProductEndpoint)

      val flash = request.flash
      val maybeOrderIdFromFlash = flash.get("orderId").map(orderId => orderId.toLong)
  
      val maybeHtml = for (
        flashOrderId <- maybeOrderIdFromFlash if flashOrderId == orderId;
        order <- orderStore.findById(flashOrderId)
      ) yield {
        val maybeViewModel = order.lineItemId match {
          case Some(itemId) => viewModelForCheckoutConfirmation(itemId)
          case None => Some { orderCompleteViewModelFactory.fromOrder(order) }
        }

        for (viewModel <- maybeViewModel) yield {
          Ok(views.html.frontend.celebrity_storefront_complete(viewModel))
        }
      }
  
      maybeHtml.flatten.getOrElse(NotFound("Order confirmation has expired."))
    }
  }

  private def viewModelForCheckoutConfirmation(lineItemId: Long) = {
    for (
      itemEntity <- lineItemStore.findEntityById(lineItemId).headOption;
      checkout <- checkoutServices.findById(itemEntity._checkoutId);
      viewModel <- orderCompleteViewModelFactory.fromEgraphPurchaseCheckout(checkout)
    ) yield {
      viewModel
    }
  }
}
