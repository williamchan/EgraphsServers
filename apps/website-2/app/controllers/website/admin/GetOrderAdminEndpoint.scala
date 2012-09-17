package controllers.website.admin

import play.api.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{Egraph, OrderStore}
import org.apache.commons.lang.StringEscapeUtils
import controllers.WebsiteControllers

private[controllers] trait GetOrderAdminEndpoint { this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def orderStore: OrderStore
  protected def controllerMethod: ControllerMethod

  def getOrderAdmin(orderId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      orderStore.findById(orderId) match {
        case Some(order) => {
          val buyer = order.buyer
          val recipient = if (order.buyerId == order.recipientId) buyer else order.recipient
          val fulfillingEgraph: Option[Egraph] = orderStore.findFulfilledWithId(orderId).map(f => f.egraph)
          val product = order.product
          val celebrityName = product.celebrity.publicName

          val fieldDefaults: (String => String) = {
            (paramName: String) => paramName match {
              case "messageToCelebrity" => StringEscapeUtils.escapeHtml(order.messageToCelebrity.getOrElse(""))
              case "requestedMessage" => StringEscapeUtils.escapeHtml(order.requestedMessage.getOrElse(""))
            }
          }

          views.html.Application.admin.admin_order(
            order = order,
            product = product,
            buyer = buyer,
            buyerEmail = buyer.account.email,
            recipient = recipient,
            recipientEmail = recipient.account.email,
            celebrityName = celebrityName,
            fulfillingEgraph = fulfillingEgraph,
            fields = fieldDefaults)
        }
        case None => NotFound("Order with id " + orderId.toString + " not found")
      }
    }
  }
}

object GetOrderAdminEndpoint {

  def url(orderId: Long) = {
    WebsiteControllers.reverse(WebsiteControllers.getOrderAdmin(orderId))
  }
}
