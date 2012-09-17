package controllers.website.admin

import play.api.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import services.Utils
import controllers.WebsiteControllers

private[controllers] trait GetCelebrityEgraphsAdminEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def controllerMethod: ControllerMethod

  protected def celebrityStore: CelebrityStore
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters

  def getCelebrityEgraphsAdmin(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity, admin) =>
        val query = filter match {
          case "passedBiometrics" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.passedBiometrics)
          case "failedBiometrics" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.failedBiometrics)
          case "approvedByAdmin" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.approvedByAdmin)
          case "rejectedByAdmin" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.rejectedByAdmin)
          case "published" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.published)
          case "awaitingVerification" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.awaitingVerification)
          case "all" => egraphStore.getCelebrityEgraphsAndResults(celebrity)
          case _ => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.pendingAdminReview)
        }
        val pagedQuery: (Iterable[(Egraph, Option[VBGVerifySample], Option[XyzmoVerifyUser])], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebrityEgraphsAdminEndpoint.url(celebrity = celebrity), filter = Some(filter))
        views.html.Application.admin.admin_egraphs(egraphAndResults = pagedQuery._1, celebrity = Some(celebrity))
    }
  }
}

object GetCelebrityEgraphsAdminEndpoint {

  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphsAdmin", Map("celebrityId" -> celebrity.id.toString))
  }
}
