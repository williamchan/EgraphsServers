package controllers.website.admin

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import models._
import scala.Some
import controllers.WebsiteControllers
import org.apache.commons.lang.StringEscapeUtils

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
          val order = orderStore.get(printOrder.orderId)
          val recipientEmail = order.recipient.account.email
          val buyerEmail = order.buyer.account.email
          val egraph = egraphStore.findByOrder(printOrder.orderId, egraphQueryFilters.publishedOrApproved).headOption

          val fieldDefaults: (String => String) = {
            (paramName: String) => paramName match {
              case "shippingAddress" => StringEscapeUtils.escapeHtml(printOrder.shippingAddress)
            }
          }

          views.Application.admin.html.admin_printorder(
            printOrder = printOrder,
            order = order,
            recipientEmail = recipientEmail,
            buyerEmail = buyerEmail,
            egraph = egraph,
            fields = fieldDefaults)
        }
        case None => NotFound("Print Order with id " + printOrderId.toString + " not found")
      }
    }
  }
}

object GetPrintOrderAdminEndpoint {

  def url(printOrderId: Long) = {
    WebsiteControllers.reverse(WebsiteControllers.getPrintOrderAdmin(printOrderId))
  }
}
