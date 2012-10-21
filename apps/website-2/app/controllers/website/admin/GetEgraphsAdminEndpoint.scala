package controllers.website.admin

import play.api.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}


private[controllers] trait GetEgraphsAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore
  
  import services.AppConfig.instance
  private def egraphQueryFilters = instance[EgraphQueryFilters]

  def getEgraphsAdmin(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
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
        val pagedQuery: (Iterable[(Egraph, Option[VBGVerifySample], Option[XyzmoVerifyUser])], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url)
        Ok(views.html.Application.admin.admin_egraphs(egraphAndResults = pagedQuery._1))
      }
    }
  }
}

object GetEgraphsAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getEgraphsAdmin().url
  }
}
