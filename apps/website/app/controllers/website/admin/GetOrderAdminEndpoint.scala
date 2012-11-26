package controllers.website.admin

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.http.ControllerMethod
import controllers.WebsiteControllers
import services.http.filters.HttpFilters
import models.{Egraph, OrderStore}
import org.apache.commons.lang3.StringEscapeUtils
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetOrderAdminEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore

  def getOrderAdmin(orderId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        orderStore.findById(orderId) match {
          case Some(order) => {
            val buyer = order.buyer
            val recipient = if (order.buyerId == order.recipientId) buyer else order.recipient
            val fulfillingEgraph: Option[Egraph] = orderStore.findFulfilledWithId(orderId).map(f => f.egraph)
            val product = order.product
            val celebrityName = product.celebrity.publicName
            
            val fieldDefaults: (String => String) = {
              (paramName: String) => paramName match {
                case "recipientName" => StringEscapeUtils.escapeHtml4(order.recipientName)
                case "messageToCelebrity" => StringEscapeUtils.escapeHtml4(order.messageToCelebrity.getOrElse(""))
                case "requestedMessage" => StringEscapeUtils.escapeHtml4(order.requestedMessage.getOrElse(""))
                case "newRecipientEmail" => StringEscapeUtils.escapeHtml4(order.requestedMessage.getOrElse(""))
              }
            }

            Ok(views.html.Application.admin.admin_order(
                order = order,
                product = product,
                buyer = buyer,
                buyerEmail = buyer.account.email,
                recipient = recipient,
                recipientEmail = recipient.account.email,
                celebrityName = celebrityName,
                fulfillingEgraph = fulfillingEgraph,
                fields = fieldDefaults))
          }
          case None => NotFound("Order with id " + orderId.toString + " not found")
        }
      }
    }
  }
}

object GetOrderAdminEndpoint {

  def url(orderId: Long) = {
    controllers.routes.WebsiteControllers.getOrderAdmin(orderId).url
  }
}
