package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.EgraphStore

private[controllers] trait GetEgraphAdminEndpoint { this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def egraphStore: EgraphStore
  protected def controllerMethod: ControllerMethod

  def getEgraphAdmin(egraphId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      egraphStore.findById(egraphId) match {
        case Some(egraph) => {
          views.Application.admin.html.admin_egraph(egraph = egraph, signatureResult = egraph.signatureResult, voiceResult = egraph.voiceResult)
        }
        case None => {
          NotFound("Egraph with id " + egraphId.toString + " not found")
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
