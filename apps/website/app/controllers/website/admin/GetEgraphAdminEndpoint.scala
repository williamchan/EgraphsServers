package controllers.website.admin

import models.{PrintOrderStore, EgraphStore}
import play.api.mvc.{Action, Controller}
import services.ConsumerApplication
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import services.mvc.ImplicitHeaderAndFooterData
import services.mvc.egraphs.EgraphView

private[controllers] trait GetEgraphAdminEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore
  protected def printOrderStore: PrintOrderStore
  protected def consumerApp: ConsumerApplication

  def getEgraphAdmin(egraphId: Long, action: String = "") = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      httpFilters.requireEgraphId(egraphId) { egraph =>
      	Action { implicit request =>
      	  if (action == "preview") {
            Ok(EgraphView.renderEgraphPage(egraph=egraph, order=egraph.order, consumerApp = consumerApp))
          } else {
            val order = egraph.order
            val buyer = order.buyer
            val recipient = if (order.buyerId == order.recipientId) buyer else order.recipient
            val recipientAccount = recipient.account
            Ok(views.html.Application.admin.admin_egraph(
              egraph = egraph,
              signatureResult = egraph.signatureResult,
              voiceResult = egraph.voiceResult,
              order = order,
              buyer = buyer,
              buyerEmail = buyer.account.email,
              recipient = recipient,
              recipientEmail = recipientAccount.email,
              maybePrintOrder = printOrderStore.findByOrderId(egraph.orderId).headOption
            ))
          }
      	}
      }
    }
  }
}

object GetEgraphAdminEndpoint {

  def url(egraphId: Long) = {
    controllers.routes.WebsiteControllers.getEgraphAdmin(egraphId).url
  }
}
