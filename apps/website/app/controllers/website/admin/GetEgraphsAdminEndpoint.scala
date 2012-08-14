package controllers.website.admin

import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.{AdminRequestFilters, ControllerMethod}
import play.mvc.Router.ActionDefinition
import services.Utils
import controllers.WebsiteControllers

private[controllers] trait GetEgraphsAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters

  def getEgraphsAdmin(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val query = filter match {
        case "passedBiometrics" => egraphStore.getEgraphsAndResults(egraphQueryFilters.passedBiometrics)
        case "failedBiometrics" => egraphStore.getEgraphsAndResults(egraphQueryFilters.failedBiometrics)
        case "approvedByAdmin" => egraphStore.getEgraphsAndResults(egraphQueryFilters.approvedByAdmin)
        case "rejectedByAdmin" => egraphStore.getEgraphsAndResults(egraphQueryFilters.rejectedByAdmin)
        case "published" => egraphStore.getEgraphsAndResults(egraphQueryFilters.published)
        case "awaitingVerification" => egraphStore.getEgraphsAndResults(egraphQueryFilters.awaitingVerification)
        case "all" => egraphStore.getEgraphsAndResults()
        case _ => egraphStore.getEgraphsAndResults(egraphQueryFilters.pendingAdminReview)
      }
      val pagedQuery: (Iterable[(Egraph, Option[VBGVerifySample], Option[XyzmoVerifyUser])], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
      WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetEgraphsAdminEndpoint.url(), filter = Some(filter))
      views.Application.admin.html.admin_egraphs(egraphAndResults = pagedQuery._1)
    }
  }
}

object GetEgraphsAdminEndpoint {

  def url(): ActionDefinition = {
    WebsiteControllers.reverse(WebsiteControllers.getEgraphsAdmin())
  }
}
