package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.OrderStore

private[controllers] trait GetOrderEndpoint { this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod

  /**
   * Serves the website's Celebrity root page.
   */
  def getOrder(orderId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      orderStore.findById(orderId) match {
        case Some(order) => views.Application.admin.html.admin_order(order = order)
        case None => NotFound("Order with id " + orderId.toString + " not found")
      }
    }
  }
}

object GetOrderEndpoint {

  def url(orderId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getOrder", Map("orderId" -> orderId.toString))
  }
}
