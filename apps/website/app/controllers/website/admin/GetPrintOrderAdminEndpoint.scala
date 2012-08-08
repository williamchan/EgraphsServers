package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models._
import scala.Some

private[controllers] trait GetPrintOrderAdminEndpoint { this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters
  protected def orderStore: OrderStore
  protected def printOrderStore: PrintOrderStore
  protected def controllerMethod: ControllerMethod

  def getPrintOrderAdmin(printOrderId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      printOrderStore.findById(printOrderId) match {
        case Some(printOrder) => {
          printOrder
          val order = orderStore.get(printOrder.orderId)
          val egraph = egraphStore.findByOrder(printOrder.orderId, egraphQueryFilters.publishedOrApproved).headOption
          views.Application.admin.html.admin_printorder(printOrder = printOrder, order = order, egraph = egraph)
        }
        case None => NotFound("Print Order with id " + printOrderId.toString + " not found")
      }
    }
  }
}

object GetPrintOrderAdminEndpoint {

  def url(printOrderId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getPrintOrderAdmin", Map("printOrderId" -> printOrderId.toString))
  }
}
