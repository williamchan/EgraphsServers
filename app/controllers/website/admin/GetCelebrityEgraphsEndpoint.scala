package controllers.website.admin

import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import services.Utils
import controllers.WebsiteControllers

private[controllers] trait GetCelebrityEgraphsEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def controllerMethod: ControllerMethod

  protected def celebrityStore: CelebrityStore
  protected def egraphStore: EgraphStore

  def getCelebrityEgraphs(page: Int = 1) = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity, admin) =>
        var query = egraphStore.getCelebrityEgraphsAndResults(celebrity)
        val pagedQuery: (Iterable[(Egraph, VBGVerifySample, XyzmoVerifyUser)], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebrityEgraphsEndpoint.url(celebrity = celebrity))
        views.Application.admin.html.admin_celebrityegraphs(celebrity = celebrity, egraphsAndResults = pagedQuery._1)
    }
  }
}

object GetCelebrityEgraphsEndpoint {

  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphs", Map("celebrityId" -> celebrity.id.toString))
  }
}
