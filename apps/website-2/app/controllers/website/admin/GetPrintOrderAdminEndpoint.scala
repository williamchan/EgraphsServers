package controllers.website.admin

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.http.ControllerMethod
import controllers.WebsiteControllers
import services.http.filters.HttpFilters
import models._
import org.apache.commons.lang3.StringEscapeUtils

private[controllers] trait GetPrintOrderAdminEndpoint { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore
  protected def orderStore: OrderStore
  protected def printOrderStore: PrintOrderStore
  
  import services.AppConfig.instance
  private def egraphQueryFilters = instance[EgraphQueryFilters]

  def getPrintOrderAdmin(printOrderId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        printOrderStore.findById(printOrderId) match {
          case Some(printOrder) => {
            val order = orderStore.get(printOrder.orderId)
            val recipientEmail = order.recipient.account.email
            val buyerEmail = order.buyer.account.email
            val egraph = egraphStore.findByOrder(printOrder.orderId, egraphQueryFilters.publishedOrApproved).headOption

            val fieldDefaults: (String => String) = {
              (paramName: String) => paramName match {
                case "shippingAddress" => StringEscapeUtils.escapeHtml4(printOrder.shippingAddress)
              }
            }

            Ok(views.html.Application.admin.admin_printorder(
                printOrder = printOrder,
                order = order,
                recipientEmail = recipientEmail,
                buyerEmail = buyerEmail,
                egraph = egraph,
            	fields = fieldDefaults))
          }
          case None => NotFound("Print Order with id " + printOrderId.toString + " not found")
        }
      }
    }
  }
}

object GetPrintOrderAdminEndpoint {

  def url(printOrderId: Long) = {
    controllers.routes.WebsiteControllers.getPrintOrderAdmin(printOrderId).url
  }
}
