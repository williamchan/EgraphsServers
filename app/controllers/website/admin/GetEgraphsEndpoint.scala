package controllers.website.admin

import org.squeryl.Query
import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.{AdminRequestFilters, ControllerMethod}
import play.mvc.Router.ActionDefinition
import services.Utils
import controllers.WebsiteControllers

private[controllers] trait GetEgraphsEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def egraphStore: EgraphStore

  def getEgraphs(page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      var query: Query[(Egraph, VBGVerifySample, XyzmoVerifyUser)] = egraphStore.getEgraphsAndResults
      val pagedQuery: (Query[(Egraph, VBGVerifySample, XyzmoVerifyUser)], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
      WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetEgraphsEndpoint.url())
      views.Application.admin.html.admin_egraphs(egraphAndResults = pagedQuery._1)
    }
  }
}

object GetEgraphsEndpoint {

  def url(): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getEgraphs")
  }
}
