package controllers.website.admin

import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
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
  protected def egraphQueryFilters: EgraphQueryFilters

  def getEgraphsAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        
        // get query parameters
        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
        val filter: String = Form("filter" -> text).bindFromRequest.fold(formWithErrors => "pendingAdminReview", validForm => validForm)
        
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
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetEgraphsAdminEndpoint.url, filter = Option(filter))
        Ok(views.html.Application.admin.admin_egraphs(egraphAndResults = pagedQuery._1))
      }
    }
  }
}

object GetEgraphsAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getEgraphsAdmin.url
  }
}
