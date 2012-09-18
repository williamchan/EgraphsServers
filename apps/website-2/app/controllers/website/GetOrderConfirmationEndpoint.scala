package controllers.website

import play.api._
import play.api.mvc._

import models._
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
  protected def controllerMethod: ControllerMethod
  protected def orderCompleteViewModelFactory: OrderCompleteViewModelFactory

  //
  // Controllers
  //
  def getOrderConfirmation(orderId: Long) = Action { implicit request =>
    controllerMethod() {
      // Get order ID from flash scope -- it's OK to just read it
      // because it can only have been provided by our own code (in this case
      // probably PostBuyProductEndpoint)
      import services.http.SafePlayParams.Conversions._
  
      val flash = request.flash
      val maybeOrderIdFromFlash = flash.get("orderId").map(orderId => orderId.toLong)
  
      val maybeHtml = for (
        flashOrderId <- maybeOrderIdFromFlash if flashOrderId == orderId;
        order <- orderStore.findById(flashOrderId)
      ) yield {
        views.html.frontend.celebrity_storefront_complete(
          orderCompleteViewModelFactory.fromOrder(order)
        )
      }
  
      maybeHtml.getOrElse(NotFound("Order confirmation has expired."))
    }
  }
}