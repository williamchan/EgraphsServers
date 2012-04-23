package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.EgraphStore
import controllers.website.GetEgraphEndpoint

private[controllers] trait GetEgraphAdminEndpoint { this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def egraphStore: EgraphStore
  protected def controllerMethod: ControllerMethod

  def getEgraphAdmin(egraphId: Long, action: Option[String] = None) = controllerMethod() {
    action match {
      case Some("preview") => {
        adminFilters.requireEgraph { (egraph, admin) =>
          GetEgraphEndpoint.html(egraph = egraph, order = egraph.order)
        }
      }

      case _ => {
        adminFilters.requireEgraph { (egraph, admin) =>
          views.Application.admin.html.admin_egraph(egraph = egraph, signatureResult = egraph.signatureResult, voiceResult = egraph.voiceResult)
        }
      }
    }
  }
}

object GetEgraphAdminEndpoint {

  def url(egraphId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getEgraphAdmin", Map("egraphId" -> egraphId.toString))
  }
}
