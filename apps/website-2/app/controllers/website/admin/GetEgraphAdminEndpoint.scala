package controllers.website.admin

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.http.ControllerMethod
import controllers.WebsiteControllers
import services.http.filters.HttpFilters
import models.EgraphStore
import controllers.website.GetEgraphEndpoint
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetEgraphAdminEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore

  def getEgraphAdmin(egraphId: Long, action: Option[String] = None) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireEgraphId(egraphId) { egraph =>
      	Action { implicit request =>
      	  // TODO(play2): I had a hard time getting url parameter and query parameters working together. Will figure out later.
          if (request.queryString.get("action").getOrElse("").toString.contains("preview")) {
            Ok(GetEgraphEndpoint.html(egraph = egraph, order = egraph.order))
          } else {
            Ok(views.html.Application.admin.admin_egraph(egraph = egraph, signatureResult = egraph.signatureResult, voiceResult = egraph.voiceResult))
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
