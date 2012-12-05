package controllers.website.admin

import play.api.mvc.{Action, Controller}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import models.{PrintOrderStore, Egraph, OrderStore}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetOrderAdminEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore
  protected def printOrderStore: PrintOrderStore

  def getOrderAdmin(orderId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        orderStore.findById(orderId) match {
          case Some(order) => {
            val buyer = order.buyer
            val recipient = if (order.buyerId == order.recipientId) buyer else order.recipient
            val recipientAccount = recipient.account
            val fulfillingEgraph: Option[Egraph] = orderStore.findFulfilledWithId(orderId).map(f => f.egraph)
            val product = order.product
            val celebrityName = product.celebrity.publicName

            val fieldDefaults: (String => String) = {
              (paramName: String) => paramName match {
                case "recipientName" => order.recipientName
                case "messageToCelebrity" => order.messageToCelebrity.getOrElse("")
                case "requestedMessage" => order.requestedMessage.getOrElse("")
                case "newRecipientEmail" => recipientAccount.email
                case "newBuyerEmail" => buyer.account.email
                case "newProductId" => order.productId.toString    
              }
            }

            Ok(views.html.Application.admin.admin_order(
                order = order,
                product = product,
                buyer = buyer,
                buyerEmail = buyer.account.email,
                recipient = recipient,
                recipientEmail = recipientAccount.email,
                celebrityName = celebrityName,
                fulfillingEgraph = fulfillingEgraph,
                maybePrintOrder = printOrderStore.findByOrderId(orderId).headOption,
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
