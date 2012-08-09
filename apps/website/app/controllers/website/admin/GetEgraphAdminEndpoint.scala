package controllers.website.admin

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import models.EgraphStore
import controllers.website.GetEgraphEndpoint
import controllers.WebsiteControllers

private[controllers] trait GetEgraphAdminEndpoint { this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def egraphStore: EgraphStore
  protected def controllerMethod: ControllerMethod

  def getEgraphAdmin(egraphId: Long, action: Option[String] = None) = controllerMethod() {
    adminFilters.requireEgraph {
      (egraph, admin) =>
        action match {
          case Some("preview") => {
            GetEgraphEndpoint.html(egraph = egraph, order = egraph.order)
          }

          case _ => {
            views.Application.admin.html.admin_egraph(egraph = egraph, signatureResult = egraph.signatureResult, voiceResult = egraph.voiceResult)
          }
        }
    }
  }
}

object GetEgraphAdminEndpoint {

  def url(egraphId: Long) = {
    WebsiteControllers.reverse(WebsiteControllers.getEgraphAdmin(egraphId))
  }
}
